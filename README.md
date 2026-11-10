# GYM VIP — aplicación móvil

Kotlin Multiplatform y Compose Multiplatform para Android e iOS. Consulta de membresía, pagos presenciales, catálogo, promociones, avisos y perfil. Sin checkout ni renovación móvil.

La app ahora incluye paneles de administrador y recepción con las cuentas del backend existente. Ambos roles pueden gestionar socios, consultar sus vigencias/pagos y registrar asistencia; solo administrador puede crear/editar planes y consultar cuentas del personal. Detalles, permisos y alcance en [Acceso por roles](docs/ACCESO_POR_ROLES.md).

## Estado actual

Base migrada a `:androidApp` + `:shared` siguiendo pharmaMobile (rama `feature/crud-productos-quispe`). Acceso por roles y núcleo del socio (Inicio, Membresía y Pagos) con `/api/v1/member-app/me`, carga compartida, caché aislada, vigencias y fechas Lima. La fase 4 añade catálogo, detalle, promociones, favoritos, avisos con lectura, FAQ, perfil y preferencias persistentes mediante los contratos REST existentes. Véase [Fase 4](docs/FASE_4_COMUNIDAD.md).

ADMIN y RECEPCION mantienen sus paneles y acceden al catálogo de consulta y preferencias. La fase 5 pulió las pantallas según los prototipos y verificó el emulador. Última verificación: **99 pruebas móviles y 144 backend aprobadas**, sin fallos ni omisiones, con integración real de las cuentas de personal y un socio ficticio en base aislada. El backend final único responde UP en 8081. Véase [Fase 5](docs/FASE_5_PULIDO_QA.md).

El [ajuste de lectura CLIENTE](docs/PROPUESTA_CATALOGO_SOCIO.md) está aplicado: productos/imágenes publicados y promociones vigentes; escrituras e historiales siguen siendo de ADMIN. También se corrigieron las lecturas de consentimiento y Avisos. El modo demo intercambiable y la compilación iOS siguen pendientes.

## Estructura

- `androidApp/`: aplicación Android, MainActivity y GymVipApplication.
- `shared/src/commonMain/`: Compose, navegación, dominio, casos de uso, Ktor, repositorios y Koin.
- `shared/src/androidMain/`: motor CIO, SQLite y tokens Android.
- `shared/src/iosMain/`: Darwin, SQLite, tokens iOS y MainViewController.
- `shared/src/commonTest/`: dominio, mappers y HTTP MockEngine.
- `shared/src/androidHostTest/`: pruebas JDBC/SQLite.
- `iosApp/iosApp.xcodeproj`: host SwiftUI que enlaza el framework estático `Shared`.

Paquete: `pe.edu.upeu.gymvip`. Android minSdk 24, compileSdk/targetSdk 36.

## Android

Requisitos: JDK 21, SDK Android 36, build tools y licencias instalados. Configure `ANDROID_HOME` o `sdk.dir` en `local.properties` (archivo local ignorado por Git). El ajuste local de `org.gradle.java.home` se conserva; adapte esa ruta a su equipo.

PowerShell:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

Reporte de pruebas: `shared/build/reports/tests/testAndroidHostTest/index.html`.

En sistemas Unix use `./gradlew` con los mismos argumentos.

## Backend y configuración

`shared/src/commonMain/kotlin/pe/edu/upeu/gymvip/AppConfig.kt` centraliza URLs, flags, zona horaria y futuro número de WhatsApp.

- Android debug: `http://10.0.2.2:8081` (emulador → equipo host).
- iOS debug: `http://localhost:8081`.
- Release: URL HTTPS de producción conservada de la base anterior; verificar el despliegue antes de distribuir.
- HTTP local se permite solo en debug; release exige HTTPS.
- El backend debe escuchar en 8081 para estas URLs. Su `application.yml` usa 8080 como fallback si no recibe PORT. La instalación local consolidada publica PostgreSQL en 127.0.0.1:15432; la app nunca se conecta directamente a la base.
- Para QA aislado, solo la compilación debug acepta `-Pgymvip.debugApi=http://10.0.2.2:8082`. Sin esa propiedad, el APK normal usa 8081.
- Un teléfono físico necesita una URL accesible desde su red y una configuración debug explícita para ese host; `10.0.2.2` es exclusivo del emulador.

Las rutas reales son `/api/v1/auth`, `/api/v1/member-app` y catálogo. No se implementan los endpoints supuestos del prompt sin contrastarlos con el servidor. Véase [contrato](docs/API_CONTRATO_ASUMIDO.md).

`MODO_DEMO=false`: acceso REST real; no se incluyen usuarios o contraseñas predefinidos dentro del APK. El modo demo intercambiable aún no está implementado. `FEATURE_FAVORITOS=true` habilita favoritos solo para socios; `FEATURE_QR=false` conserva una pantalla informativa y `FEATURE_SERVICIOS=false` oculta servicios. WhatsApp no tiene número configurado, por lo que ayuda dirige a recepción sin generar un enlace vacío. Tema y avisos emergentes se guardan por dispositivo y se conservan al cerrar sesión; los datos privados de la cuenta se eliminan.

## iOS (macOS/Xcode)

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
open iosApp/iosApp.xcodeproj
```

Seleccione el target GymVip, un simulador y ejecute. El build phase de Xcode llama a `:shared:embedAndSignAppleFrameworkForXcode`. Para dispositivo físico configure su equipo de firma en Xcode. El proyecto host y el target iOS no han sido compilados en Windows.

## Documentación

- [Decisiones, alcance y pendientes](docs/DECISIONES.md)
- [Arquitectura](docs/ARQUITECTURA.md)
- [Contrato REST contrastado con el código](docs/API_CONTRATO_ASUMIDO.md)
- [Pruebas y checklist de demo](docs/PRUEBAS.md)
- [Comercial, comunicación y límites de fase 4](docs/FASE_4_COMUNIDAD.md)
- [Pulido visual y QA de fase 5](docs/FASE_5_PULIDO_QA.md)

La rama personal y la publicación se realizan al final, conforme a la indicación del usuario.
