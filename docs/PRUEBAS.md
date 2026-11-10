# Pruebas — fases 1 a 5

## Fase 5: resultado final

9 nov 2026: `:androidApp:assembleDebug :androidApp:lintDebug :shared:testAndroidHostTest --offline --console=plain` terminó BUILD SUCCESSFUL en 30 s; **99 pruebas, 0 fallos, 0 omitidas** con `GYMVIP_LIVE_AUTH=1` y `GYMVIP_LIVE_SOCIO_QA=1`. Lint: 0 errores. Se incluyeron dos cuentas reales de personal en 8081 y una cuenta ficticia CLIENTE en 8082/base aislada; contraseñas solo en variables de proceso. Backend: `mvnw.cmd -o package`, **144 pruebas, 0 fallos, 0 errores, 0 omitidas**. APK normal final: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`, backend 8081. El emulador verificó arranque en frío, paneles de personal, activación y recorrido del socio, estados de vigencia, tema claro/oscuro y fuente grande. Detalle, aislamiento y límites en [Fase 5](FASE_5_PULIDO_QA.md). La instalación local quedó con un backend GYM VIP activo y PostgreSQL aparte en 15432.

## Fase 4: resultado actual

Actualización después de la autorización del ajuste backend: Docker/PostgreSQL recuperados; imagen local `gymvip-backend:catalogo-socios` activa en 8081 y `/api/v1/health` UP. Backend: `mvnw.cmd package`, **142 pruebas aprobadas**, sin fallos, errores ni omisiones, incluida lectura CLIENTE y productos/imágenes ocultos con PostgreSQL aislado. Móvil: `:shared:testAndroidHostTest --offline --console=plain` con `GYMVIP_LIVE_AUTH=1`, **BUILD SUCCESSFUL en 22 s, 95 aprobadas, 0 fallos, 0 omitidas**. Las dos cuentas reales de personal completaron login/restauración, lecturas comerciales/gestión permitidas, denegaciones por rol y logout. No se alteraron sus contraseñas ni roles. El registro que sigue corresponde a la entrega inicial y explica los fallos anteriores al restablecimiento.

9 nov 2026: `:androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain` terminó **BUILD SUCCESSFUL en 44 s**. JUnit informa 95 casos: **93 aprobados, 0 fallos y 2 omitidos** (integración real optativa). APK generado en `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

29 casos nuevos: `ComunidadRepositoryTest` (20), `ComunidadViewModelTest` (5) y `PreferenciasComunidadTest` (4). Comprueban filtros HTTP, mapeo monetario/snake_case, ocultos por rol, promociones finalizadas, límite de notas, confirmación de escrituras, lectura/contador sin optimismo, aislamiento/propietario, cierre/expiración, respuestas tardías, operaciones en espera, cambio de token antes del envío, carga/vacío/error/reintento, separación de personal y persistencia de preferencias.

Se ampliaron los dos casos de `BackendRealAuthTest` para leer catálogo/detalles/imágenes/promociones y denegar rutas privadas a personal. Con `GYMVIP_LIVE_AUTH=1`, el intento real de esta fase completó entonces 93 casos con 2 fallos de conexión en esas pruebas: localhost:8081 rechazaba conexiones. Posteriormente se añadieron dos regresiones de cambio de sesión y se ejecutó la suite local completa de 95 casos con éxito. Docker Desktop no pudo arrancar su Secrets Engine por acceso denegado a `engine.sock`. No se afirma validación real nueva aprobada.

Para repetir la prueba real, iniciar los servicios locales existentes e inyectar `GYMVIP_LIVE_AUTH=1`, `GYMVIP_TEST_ADMIN_EMAIL`, `GYMVIP_TEST_ADMIN_PASSWORD`, `GYMVIP_TEST_RECEPCION_EMAIL` y `GYMVIP_TEST_RECEPCION_PASSWORD` solo al proceso; opcional `GYMVIP_TEST_BASE_URL`. Ejecutar el mismo comando. No guardar credenciales en archivos ni añadirlas al APK. Las pruebas cierran sus sesiones y no escriben catálogo ni datos de socios.

ADB inició correctamente, pero `adb devices` devolvió una lista vacía; no se revisó visualmente fase 4. Falta la prueba positiva CLIENTE, resolver su permiso global de catálogo, configurar WhatsApp oficial y validar iOS en macOS. Alcance y recorrido en `FASE_4_COMUNIDAD.md`.

## Fase 3: núcleo del socio

BUILD SUCCESSFUL en 20 s con `:androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain`; 66 pruebas aprobadas, 0 fallos y 0 omitidas. Incluye 21 nuevas pruebas de mapeo, caché, estado de UI, vigencia e identidad. Se volvieron a ejecutar las dos pruebas reales de personal y se verificó que esos roles no acceden a `/member-app/me`.

El workspace se contrastó con una lectura real autorizada de administrador; el recorrido positivo autenticado CLIENTE sigue pendiente de credenciales de socio. No había dispositivos conectados para revisión visual. Detalle y alcance de cada prueba en `FASE_3_SOCIO.md`.

## Ampliación posterior: acceso por roles

BUILD SUCCESSFUL en 47 s con `:androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain`: 45 pruebas aprobadas, sin fallos ni omisiones. Las dos pruebas reales ahora comprueban entrada ADMIN/RECEPCION y lecturas permitidas, restauración de sesión y la prohibición real de `/accounts` para recepción. La antigua expectativa de rechazo de todo personal fue reemplazada por la nueva autorización del usuario. Los roles desconocidos siguen rechazados. Ver `ACCESO_POR_ROLES.md` para la matriz y límites de QA.

## Resultado de fase 2

`./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain`: APK generado y 42 pruebas aprobadas, 0 fallos, incluidas dos pruebas contra el backend real con las cuentas de personal autorizadas. Las credenciales se inyectaron solo al proceso de pruebas. Véase `FASE_2_ACCESO.md` para reproducirlas y distinguir la cobertura real de la simulada.

Nuevas suites: AccesoTest (5), AuthViewModelTest (3), RepositorioAccesoTest (5), BackendRealAuthTest (2). Cubren requisitos reales de contraseña, cancelación, confidencialidad de errores, token actual por origen, 401 sin refresh, primer cambio obligatorio, consentimiento leído después de guardar, perfil no inventado, restauración y rechazo/revocación de sesiones de personal.

APK final: BUILD SUCCESSFUL en 20 s. Instalado y abierto en emulator-5554; formulario verificado visualmente al reanudar. El arranque en frío quedó condicionado por un problema de ventanas ocultas reproducido también en Ajustes de Android del mismo emulador API 37.1. Ver detalles en FASE_2_ACCESO.md; no se alteró su configuración global.

Durante el desarrollo las pruebas detectaron una comparación incorrecta entre puertos implícitos de URLBuilder y Url; se corrigió normalizando primero la URL. Las 42 pruebas se ejecutaron nuevamente con éxito después de la corrección.

## Registro histórico de fase 1

Verificado el 9 nov 2026 en Windows, JDK 21 y SDK Android 36.

## Comandos y resultados reales

```powershell
.\gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain
```

- Primer intento: error de compilación por paquete generado de SQLDelight. Se trasladó el esquema a `sqldelight/pe/edu/upeu/gymvip/data/local`.
- Segundo intento: BUILD SUCCESSFUL en 1 min 26 s.
- Verificación final tras cambios visuales: BUILD SUCCESSFUL en 22 s; 70 tareas, 12 ejecutadas y 58 al día.
- 27 pruebas, 0 fallos y 0 errores en seis suites. Se conservan las pruebas existentes y se ejecutan tras la migración.

| Suite | Pruebas |
|---|---:|
| DtoMapperTest | 4 |
| KtorMockEngineTest | 11 |
| SyncQueueManagerTest (JDBC, Android host) | 3 |
| ManageFavoritesUseCaseTest | 3 |
| MembershipValidationTest | 3 |
| MoneyPenTest | 3 |

APK generado: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Reporte HTML: `shared/build/reports/tests/testAndroidHostTest/index.html`.
Resultados JUnit XML: `shared/build/test-results/testAndroidHostTest/`.

Advertencias no bloqueantes: clases expect/actual beta, APIs Material heredadas deprecadas y proveedor SLF4J ausente en pruebas (logger NOP). Material3 y Lifecycle del catálogo de referencia son versiones preliminares; véase DECISIONES.md.

La prueba con adb no pudo enumerar dispositivos: el servidor ADB no arrancó en el entorno restringido. No se verificaron lanzamiento ni apariencia en emulador/dispositivo. No se afirma fidelidad visual completa.

## Límites

- Los tests con MockEngine no prueban el servidor real. La integración autenticada, el flujo de primer cambio de contraseña y el aislamiento de sesiones aún deben auditarse.
- No se compila iOS en Windows. En macOS ejecutar `./gradlew :shared:iosSimulatorArm64Test` y `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`, y abrir el proyecto Xcode.
- No se ha generado ni firmado una distribución release.

## Checklist manual pendiente para fases 2–5

- [ ] Onboarding de primera ejecución, saltar, login y activación.
- [ ] Acceso solo CLIENTE, credenciales erróneas e inactividad sin revelar información sensible.
- [ ] Primer cambio obligatorio, nueva contraseña y consentimiento real.
- [ ] Inicio → membresía → historial de pagos → volver.
- [ ] Estados vigente, próxima a vencer, vencida y sin membresía; último día completo en Lima.
- [ ] Membresía vencida conserva acceso a consulta.
- [ ] Catálogo → búsqueda/filtros → producto/presentación → promociones vigentes/finalizadas.
- [ ] Favoritos por cuenta y límite de 250 caracteres.
- [ ] Avisos → detalle → lectura persistida y contador actualizado.
- [ ] Perfil enmascarado → preferencias → FAQ → WhatsApp con número oficial.
- [ ] Cierre de sesión → limpieza → ingreso con otra cuenta sin datos cruzados.
- [ ] Carga, vacío, error de servidor y sin red con/sin caché en cada pantalla.
- [ ] Atrás, rotación/restauración, fuentes grandes, insets y tema claro/oscuro.

Repetir el recorrido con cada escenario demo cuando se implementen sus repositorios: vigente, próxima a vencer, vencida, sin membresía, vacío, error de servidor y offline. Actualmente el modo demo intercambiable no está implementado.
