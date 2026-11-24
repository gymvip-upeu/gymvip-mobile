# GYM VIP — Aplicación Móvil del Socio (`gymvip-mobile`)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)]()
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.1-green.svg)]()
[![Android Target](https://img.shields.io/badge/Android-API%2034%20(minSdk%2024)-orange.svg)]()
[![iOS Target](https://img.shields.io/badge/iOS-arm64%20ready-lightgrey.svg)]()
[![Branch](https://img.shields.io/badge/branch-ACOSTA--MEJORAS--Y--ACTUALIZACIONES-yellow.svg)]()
[![License](https://img.shields.io/badge/license-Academic%20Proprietary-lightgrey.svg)]()

> **Ubicación en el Workspace:** `gymvip-mobile/`  
> **Organización Oficial:** [gymvip-upeu/gymvip-mobile](https://github.com/gymvip-upeu/gymvip-mobile)  
> **Documentación Centralizada:** `../documentos/`  
> **Fecha de Actualización:** 23 de Noviembre de 2026 | Revisión Canónica v2.5

---

## 1. Nombre del Componente
**GYM VIP Mobile Multiplatform Client Application** (`gymvip-mobile`).

---

## 2. Propósito
Proveer a los socios de **VIP CENTER FIT** un canal móvil nativo de autoservicio para la consulta autoritativa de su membresía y vigencia, generación de credencial digital con QR temporal para torniquetes, compra de suplementos con carrito persistido, seguimiento de pedidos mediante ticket de retiro QR, recepción de recordatorios push y gestión de privacidad.

---

## 3. Responsabilidades
- **Autenticación Móvil y Biometría:** Inicio de sesión seguro con JWT, renovación de tokens, revocación remota de otras sesiones y bloqueo biométrico local por huella/rostro.
- **Acceso por Torniquete (QR Invertido):** Escaneo nativo del desafío QR de recepción con la cámara del dispositivo o presentación de carnet digital dinámico.
- **Catálogo y Carrito de Suplementos:** Navegación por categorías, carrito de compras sincronizado en servidor, reserva de stock por 30m y emisión de ticket de retiro QR (`rtk_...`).
- **Pagos Digitales:** Integración con pasarela CulqiLink para pago de membresías y renovación móvil.
- **Buzón y Notificaciones Push:** Recepción en segundo plano mediante Firebase Cloud Messaging (FCM) y visualización offline en bandeja de avisos.
- **Soporte Offline:** Persistencia local cifrada con SQLDelight SQLite para consulta de estado y carnet sin conexión a internet.
- **Protección LPDP (Ley N° 29733):** Canal formal de solicitud de supresión de cuenta y olvido de datos personales.

---

## 4. Arquitectura
Implementa **Clean Architecture + Model-View-ViewModel (MVVM)** en Kotlin Multiplatform:
- **`domain/` (Lógica Pura de Negocio):** Modelos inmutables (`MemberProfile`, `MembershipSummary`, `CartItem`, `MoneyPen`), contratos de repositorios y casos de uso sin acoplamiento a frameworks.
- **`data/` (Infraestructura y Persistencia):** Implementaciones concretas de repositorios, cliente Ktor HttpClient con interceptores de cabeceras Bearer, base de datos local SQLite mediante SQLDelight y almacenamiento seguro `SecureTokenStore` (expect/actual).
- **`presentation/` (UI Declarativa):** Pantallas en Compose Multiplatform con temas dinámicos Luxury Dark & Gold, componentes reutilizables y ViewModels gobernados por `StateFlow`.
- **`di/` (Inyección de Dependencias):** Composition Root ligero (`AppContainer`) por constructor sin reflexión pesada.

---

## 5. Tecnologías y Versiones Reales
| Tecnología | Versión Oficial | Propósito |
|---|---|---|
| **Kotlin** | 2.1.0 | Lenguaje principal multiplataforma |
| **Compose Multiplatform** | 1.7.1 | UI declarativa unificada Android / iOS |
| **Ktor Client** | 3.0.1 | Motor de red HTTP reactivo con serialización JSON |
| **SQLDelight** | 2.0.2 | Persistencia relacional local SQLite type-safe |
| **Android SDK** | Compile SDK 34 / minSdk 24 | Target nativo para dispositivos Android 7.0+ |
| **Firebase BoM** | 33.7.0 | Crashlytics, Performance Monitoring y FCM en Android |
| **AndroidX Biometric** | 1.2.0-alpha05 | Autenticación biométrica nativa por hardware |

---

## 6. Estructura de Carpetas
```text
gymvip-mobile/
├── gradle / gradlew.bat         # Gradle Wrapper (8.11.1)
├── build.gradle.kts             # Configuración raíz de build y plugins KMP
├── settings.gradle.kts          # Inclusión de proyectos y repositorios
├── CONTRIBUTING.md              # Estándar obligatorio de commits
├── README.md                    # Esta documentación canónica
└── composeApp/
    ├── build.gradle.kts         # Dependencias, targets KMP y firma release
    └── src/
        ├── commonMain/kotlin/com/gymvip/mobile/
        │   ├── di/              # AppContainer e inyección de dependencias
        │   ├── domain/          # Entidades, errores tipados, use cases
        │   ├── data/            # Ktor API, SQLDelight, repositorios
        │   └── presentation/    # Screens, ViewModels, Theme, Nav
        ├── androidMain/         # MainActivity, BiometricPrompt, FCM Service
        └── iosMain/             # Bindings nativos y configuración iOS
```

---

## 7. Funcionalidades
1. **Splash & Onboarding:** Bienvenida visual guiada de 4 etapas y detección automática de sesión previa.
2. **Carnet Digital y QR de Acceso:** Credencial con foto, estado de membresía en tiempo real y código QR dinámico.
3. **Tienda y Carrito de Suplementos (HU-50):** Carrito persistido, reserva de inventario, registro de comprobante y visor de ticket QR de entrega.
4. **Buzón de Comunicaciones:** Notificaciones de corte, bienvenida y anuncios generales del gimnasio.
5. **Seguridad y Biometría:** Desbloqueo por huella dactilar, cierre de sesiones concurrentes y eliminación de cuenta ARCO.
6. **Modo Offline:** Banner de desconexión y visualización de carnet desde caché local segura.

---

## 8. Usuarios y Roles Correspondientes
- **`CLIENTE` (Socio VIP):** Acceso exclusivo a la aplicación móvil. El personal operativo (`ADMIN`, `RECEPCION`) cuenta con una pantalla informativa orientándolos a usar la consola web administrativa.

---

## 9. Requisitos Previos
- **JDK:** OpenJDK 21 LTS (`JAVA_HOME` configurado).
- **Android SDK:** Command-line Tools y SDK Platform API 34 instalados.
- **Gradle:** Versión 8.11.1 (gestionada por el wrapper `gradlew`).
- **Dispositivo de Prueba:** Teléfono Android físico con depuración USB o emulador API 24 a 34.

---

## 10. Instalación desde Cero
```powershell
# 1. Clonar el repositorio oficial
git clone https://github.com/gymvip-upeu/gymvip-mobile.git
Set-Location gymvip-mobile

# 2. Situarse en la rama de trabajo
git checkout ACOSTA-MEJORAS-Y-ACTUALIZACIONES

# 3. Configurar Android SDK en local.properties
Add-Content local.properties "sdk.dir=C:\\Users\\XD\\AppData\\Local\\Android\\Sdk"
```

---

## 11. Variables de Entorno y Propiedades
Para compilaciones release firmadas, configurar `keystore.properties` (ignorado en Git):
```properties
storeFile=../release.keystore
storePassword=placeholder_keystore_pass
keyAlias=gymvip_release
keyPassword=placeholder_key_pass
```

---

## 12. Configuración Local
En `composeApp/src/commonMain/kotlin/com/gymvip/mobile/data/remote/api/ApiConstants.kt`, la URL base apunta por defecto al entorno productivo:
- **Producción:** `https://gymvip-backend.onrender.com`
- **Emulador Android Local:** `http://10.0.2.2:8081`

---

## 13. Comandos de Ejecución
```powershell
# Compilar e instalar APK de desarrollo en dispositivo conectado
.\gradlew.bat :composeApp:installDebug
```

---

## 14. Comandos de Compilación
```powershell
# Generar APK Release universal optimizado con ProGuard/R8
.\gradlew.bat :composeApp:assembleRelease

# El paquete final se generará en:
# composeApp/build/outputs/apk/release/composeApp-release.apk
```

---

## 15. Pruebas
```powershell
# Ejecutar la suite completa de pruebas unitarias KMP (50 tests)
.\gradlew.bat :composeApp:testDebugUnitTest
```

---

## 16. Integraciones
- **Backend GYM VIP:** Consumo de endpoints `/api/v1/member-app/*` y `/api/v1/auth/*`.
- **Firebase:** FCM para notificaciones push en tiempo real y Crashlytics para reporte seguro de fallos.
- **Hardware Nativo:** Cámara para lectura QR y sensor biométrico AndroidX.

---

## 17. Seguridad
- Almacenamiento cifrado de tokens JWT en Android Keystore (`EncryptedSharedPreferences`).
- Exclusión estricta de credenciales en logs de red.
- Invalidación de credenciales locales ante cierre de sesión o eliminación de cuenta.

---

## 18. Despliegue
- El binario release compilado se publica como APK universal autónomo en el portal web institucional (`/descargar`).
- **Versión Actual:** `v1.1.3` (`versionCode = 4`, `versionName = "1.1.3"`).
- **Firma Digital:** Certificado RSA 2048 verificado con esquemas v1, v2 y v3 mediante `apksigner`.

---

## 19. Solución de Problemas Frecuentes
- **Error `INSTALL_FAILED_UPDATE_INCOMPATIBLE`:** Existe una versión anterior instalada firmada con una clave distinta (ej. debug). Desinstalar la versión previa antes de instalar la release.
- **Error de Conexión en Emulador:** Si apunta a `localhost`, cambiar a `10.0.2.2` para resolver el host de desarrollo.

---

## 20. Limitaciones Conocidas
- El soporte iOS está validado en código fuente compartido Kotlin; la generación de paquetes `.ipa` requiere entorno macOS con Xcode autorizado.

---

## 21. Enlaces hacia los Otros Repositorios
- **Backend API:** [gymvip-backend](https://github.com/gymvip-upeu/gymvip-backend)
- **Frontend Web:** [gymvip-frontend](https://github.com/gymvip-upeu/gymvip-frontend)
- **Comunicaciones:** [gymvip-ms-comunicaciones](https://github.com/gymvip-upeu/gymvip-ms-comunicaciones)
- **Portal Público:** [gymvip-Landing-Page](https://github.com/gymvip-upeu/gymvip-Landing-Page)

---

## 22. Enlaces hacia Diagramas
- **Flujos Móviles y Torniquete:** `../documentos/diagramas/09-Flujos-Funcionales/`
- **Informe de Estado Mobile:** `../documentos/mobile/ESTADO_APLICACION_MOVIL.md`

---

## 23. Enlaces hacia ESTADO_PROYECTO.md
- Documento Central: [`../documentos/ESTADO_PROYECTO.md`](../documentos/ESTADO_PROYECTO.md)

---

## 24. Fecha y Revisión de Referencia
- **Fecha:** 23 de Noviembre de 2026.
- **Revisión de Ingeniería:** Consolidación Maestra Sprint 1 & 2 / Release Candidate v2.5.
- **Autoría:** Equipo de Ingeniería GYM VIP / VIP CENTER FIT.
