# Acceso móvil de socios, administrador y recepción

9 nov 2026, America/Lima.

El usuario amplió explícitamente el alcance móvil después de la fase 2 para incluir interfaces ADMIN y RECEPCION. Esta decisión reemplaza la restricción histórica de móvil solo CLIENTE. Se utilizan las mismas cuentas y permisos existentes en el backend; no se modificaron roles, contraseñas ni código del servidor.

## Interfaces incluidas

| Función | Socio | Recepción | Administrador |
|---|---|---|---|
| Espacio personal de socio y primer cambio obligatorio | Sí | No se consulta | No se consulta |
| Panel con total de socios y vigencias con alerta | — | Sí | Sí |
| Búsqueda paginada de socios | — | Sí | Sí |
| Registrar y editar datos de socios | — | Sí | Sí |
| Consultar ficha, vigencia y pagos registrados | Solo propios en su flujo existente | Sí | Sí |
| Registrar asistencia con validación de vigencia | — | Sí | Sí |
| Consultar planes, beneficios y condiciones | Flujo existente | Sí | Sí |
| Crear y editar planes | — | No | Sí |
| Consultar cuentas y roles del personal | — | No | Sí |
| Cerrar sesión y limpiar datos locales | Sí | Sí | Sí |

El panel de personal tiene Inicio, Socios, Planes y Perfil. Administrador tiene además Cuentas y los formularios de edición de planes. Se conservó el diseño oscuro/dorado y los estados de carga, vacío, error y nuevo intento. Las operaciones de registro y edición esperan confirmación de éxito del servidor; no presentan un guardado fallido como exitoso. Editar socios envía la versión real de la ficha para que el servidor controle conflictos 409. Asistencia solicita confirmación y respeta el permiso de cobertura devuelto por el servidor.

El alcance de esta ampliación no replica todas las funciones de la web: no se añadieron creación de cuentas del personal, cambio de roles, ventas/renovaciones, anulaciones de periodos, importaciones ni administración del catálogo comercial. No hay botones que simulen estas operaciones.

## Autenticación y autorización

- Login compartido, sin credenciales prellenadas ni contraseñas incorporadas al APK.
- CLIENTE mantiene su sesión y primer cambio obligatorio. El personal nunca consulta `member-app/session` ni recibe un perfil ficticio de socio.
- ADMIN/RECEPCION validan sus permisos con GET `/api/v1/auth/me` antes de abrir el panel. Al restaurar se vuelven a consultar los roles al servidor; no se confía en un rol persistido localmente.
- Un rol desconocido se rechaza y se solicita cerrar su token.
- Si la cuenta tiene varios roles reconocidos, ADMIN habilita el panel administrativo; en su ausencia RECEPCION habilita atención.
- `GestionPersonalRepositoryRest` exige sesión de personal para cualquier operación, y ADMIN para guardar planes o consultar cuentas, incluso si se llama fuera de la UI.
- El backend mantiene sus controles `@PreAuthorize`. Ocultar controles no reemplaza la autorización del servidor.
- Cierre de sesión invalida el acceso local inmediatamente y solicita la revocación remota con el token capturado; los datos y ViewModels de personal no se comparten con otro usuario.

## Contratos reutilizados

| Método / ruta | Uso |
|---|---|
| GET `/api/v1/auth/me` | Correo y roles vigentes del usuario |
| GET `/api/v1/clients?query&page&size` | Búsqueda paginada de socios |
| POST `/api/v1/clients` | Registrar socio |
| GET/PUT `/api/v1/clients/{id}` | Consultar y actualizar ficha con versión |
| GET `/api/v1/coverage?state=ALERTAS` | Total de vigencias con alerta |
| GET `/api/v1/coverage/clients/{id}` | Vigencia autoritativa y permiso de asistencia |
| GET `/api/v1/members/{id}` | Pagos registrados del socio |
| POST `/api/v1/members/{id}/attendance` | Registrar asistencia |
| GET `/api/v1/plans` | Consultar planes |
| POST/PUT `/api/v1/plans[/{id}]` | Crear/editar plan, ADMIN |
| GET `/api/v1/accounts` | Consultar cuentas del personal, ADMIN |

## Verificación realizada

Comando: `./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain`.

BUILD SUCCESSFUL en 47 segundos; 45 pruebas, 0 fallos y 0 omitidas. Se habilitaron las dos pruebas reales inyectando las credenciales autorizadas solo al proceso de ejecución; no se escribieron en archivos del proyecto.

Las dos pruebas reales iniciaron sesión con las cuentas proporcionadas, validaron restauración y lecturas de resumen, socios y planes. ADMIN pudo consultar cuentas. RECEPCION fue bloqueada por el repositorio local y, saltando ese guard en la prueba, recibió Forbidden del backend real en `/accounts`. Al terminar se cerraron las sesiones de prueba. No se alteraron registros ni permisos de usuarios reales.

Las pruebas controladas comprueban además: rechazo de rol desconocido, ausencia de consultas a member-app para personal, actualización de rol ADMIN → RECEPCION al restaurar, bloqueo de guardar planes y cuentas para recepción sin enviar la petición y bloqueo del repositorio de personal sin sesión de personal.

Los formularios de registro/edición y asistencia están conectados a los contratos inspeccionados; no se ejecutaron escrituras contra datos reales durante QA. La revisión visual de los nuevos paneles queda pendiente: el emulador anterior ya no estaba conectado al intentar instalar esta entrega. iOS requiere validación en macOS/Xcode.

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Archivos principales

Ampliación de fase 4: ADMIN y RECEPCION abren catálogo/promociones de consulta y preferencias desde sus paneles. Favoritos/avisos/FAQ de socio permanecen fuera del panel de personal. No se añadieron escrituras comerciales. Tras el ajuste autorizado y la actualización del backend local, las dos cuentas reales de personal completaron las pruebas nuevas y la suite móvil aprobó 95 casos. Backend aprobó 142 pruebas. Ver `FASE_4_COMUNIDAD.md` y `PRUEBAS.md` para la evidencia actual.

- Dominio: `Rol.kt`, `GestionPersonal.kt`, `GestionPersonalRepository.kt`.
- Red y datos: `AuthApi.kt`, `PersonalApi.kt`, `PersonalDtos.kt`, `AuthRepositoryImpl.kt`, `GestionPersonalRepositoryRest.kt`.
- Presentación: `AuthViewModel.kt`, `PersonalViewModel.kt`, `PersonalScreen.kt`, `FormulariosPersonal.kt`, `App.kt`.
- Pruebas: `RepositorioAccesoTest.kt` y `BackendRealAuthTest.kt`.
