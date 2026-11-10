# Entrega de fase 2 — acceso conectado al backend

> Registro histórico. Posteriormente el usuario autorizó interfaces móviles para ADMIN y RECEPCION. El rechazo de personal descrito en esta fase fue reemplazado por el acceso y la matriz de `ACCESO_POR_ROLES.md`.

9 nov 2026, America/Lima.

## Implementación

- Onboarding de dos páginas con logo GV, degradado propio, Saltar y acceso/activación. Se recuerda la elección por dispositivo.
- Login con correo, contraseña con ojo, botón deshabilitado si faltan datos, progreso, ayuda y errores sin revelar si existe la cuenta. Recordar correo guarda solo el identificador.
- Activación en dos pasos: credenciales temporales y nueva contraseña con confirmación, indicador de formato y consentimiento. El enlace legal indica que el texto es provisional.
- Estado independiente de primer cambio: una sesión temporal nunca abre Inicio ni carga el perfil protegido.
- Consulta del consentimiento persistido antes de cambiar la contraseña. Nueva autenticación después de que el servidor revoca los tokens.
- Sesión persistente y revalidada al iniciar; un 401 autenticado vuelve a acceso y limpia datos locales propios. No se inventa un endpoint refresh.
- Navegación protegida durante restauración y limpieza de ViewModels al salir de la sesión.
- Tema Android propio de arranque, barras de sistema claras sobre fondo oscuro y campos con superficie oscura.

Archivos principales: `AuthRepositoryImpl.kt`, `SesionLocal.kt`, `GymVipHttpClient.kt`, `AuthApi.kt`, `MemberApi.kt`, `AuthViewModel.kt`, `LoginScreen.kt`, `ActivationScreen.kt`, `OnboardingScreen.kt` y los almacenes de cada plataforma.

## Verificación

El APK Android compila con `:androidApp:assembleDebug`. Las pruebas se ejecutan con `:shared:testAndroidHostTest`.

Se ejecutaron 42 pruebas: las 27 anteriores, 13 nuevas pruebas de acceso y 2 pruebas reales contra el backend. Las pruebas reales reciben las credenciales únicamente mediante variables del proceso; no se guardan en archivos del proyecto ni en el APK. Los usuarios autorizados devolvieron ADMIN y RECEPCION. Se confirmó que AuthRepositoryImpl rechaza esos roles para el móvil y no conserva su token.

La activación de un CLIENTE se prueba con MockEngine y SQLite: secuencia de endpoints, bloqueo del perfil antes del cambio, confirmación del consentimiento, revocación y segundo login con token nuevo. No se ejecutó un cambio de contraseña de usuario real ni se creó un socio de prueba, porque no se facilitó una cuenta CLIENTE.

Para habilitar las dos pruebas reales, inyectar antes de Gradle (sin poner valores privados en archivos):

- `GYMVIP_LIVE_AUTH=1`
- `GYMVIP_TEST_ADMIN_EMAIL`, `GYMVIP_TEST_ADMIN_PASSWORD`
- `GYMVIP_TEST_RECEPCION_EMAIL`, `GYMVIP_TEST_RECEPCION_PASSWORD`
- `GYMVIP_TEST_BASE_URL` opcional, por defecto `http://localhost:8081` para las pruebas JVM.

Sin esas variables, las dos pruebas reales se omiten expresamente. El cambio de `GYMVIP_LIVE_AUTH` invalida la caché de la tarea de pruebas. Los tests no modifican contraseñas ni roles y cierran los tokens de prueba.

Reportes: `shared/build/reports/tests/testAndroidHostTest/index.html` y `shared/build/test-results/testAndroidHostTest/`.

Verificación final: BUILD SUCCESSFUL en 20 s, 42 pruebas sin fallos ni omisiones (se habilitaron las dos pruebas reales). APK instalado en emulator-5554 (Pixel 8 Pro, API 37.1), Activity iniciada sin errores AndroidRuntime.

La revisión visual encontró un problema de transiciones en el emulador: en arranque en frío la superficie de la ventana permanece oculta aunque la jerarquía Compose contiene los controles. El mismo negro se reprodujo al abrir la app Ajustes del sistema. Salir a Inicio y reanudar GYM VIP permitió ver el formulario de login; se verificaron distribución, campos, ojo, ayuda y botón deshabilitado sin credenciales. El tema nativo y las barras de sistema se ajustaron al diseño oscuro. No se afirma que el arranque en frío esté validado en este emulador ni en un dispositivo físico. Se conservó la configuración global del emulador.

## Límites y próximos pasos

- Falta una cuenta CLIENTE para validar la activación completa en servidor y el acceso a Inicio con datos reales.
- No se alteró el código del backend ni las cuentas proporcionadas. Se iniciaron los contenedores existentes de backend y PostgreSQL para las pruebas.
- El endurecimiento de iOS con Keychain y su build nativo requieren una fase posterior en macOS.
- El modo demo intercambiable y el pulido de las pantallas internas siguen pendientes de las fases siguientes.
