# Decisiones de implementación

> Cambio de alcance posterior a fase 2: por instrucción explícita del usuario, ADMIN y RECEPCION ahora tienen interfaces móviles propias. La restricción histórica de acceso exclusivo CLIENTE queda reemplazada por la matriz de `ACCESO_POR_ROLES.md`. Los permisos del backend se conservan.

Fecha: 9 nov 2026 (America/Lima).

## Actualización de fase 3

Inicio, Membresía y Pagos comparten `SocioViewModel` y una respuesta `/member-app/me`. El repositorio valida identidad y token antes/después de la red, mantiene una instantánea en memoria de la sesión y transacciona las tablas SQLite existentes. No se añadió sincronización bidireccional ni se alteró el esquema. Se eliminaron los fallbacks a `/api/v1/me/membership` y `/api/v1/me/payments`, ausentes en el backend.

Se reconocen los estados reales POR_VENCER/VENCIDO, cancelación y cobertura futura. Las fechas de fin de periodos son exclusivas y se muestran como último día incluido; estado y días del servidor prevalecen. No se asume una duración de 30 días si falta el dato. Se conservan transferencias y notas/planes de pagos, sin el badge inventado CONCILIADO. Las promociones de Inicio se integrarán en fase 4; no se muestran ofertas ni avisos fijos de ejemplo.

Se mantiene la separación móvil de los tres roles. Ver `FASE_3_SOCIO.md` para decisiones, 66 pruebas y límites de validación real/visual.

## Alcance y fuentes

Se aplican el backlog, las tareas y los prototipos adjuntos, junto con el prompt maestro por fases. Esta entrega corresponde a la fase 1 (base); no implica que los flujos existentes cumplan todavía el contrato real. El repositorio móvil ya contenía una aplicación en `composeApp`, y el backend ya dispone de controladores. Se conserva esa implementación y sus pruebas al migrar a `androidApp` + `shared`, sin reescribir ni modificar el backend.

Referencia consultada: `123456jor/pharmaMobile-`, rama `feature/crud-productos-quispe`, catálogo SHA `c09af56c158d2854ff2806915cd98b83f4cf8dd4`.

## D-01 a D-06: reglas del producto

- D-01: correo como identificador previsto. `AppConfig.IDENTIFICADOR_LOGIN` concentra la selección; la adaptación del login existente corresponde a la fase 2.
- D-02: sin checkout, pagos online ni renovaciones móviles; contacto por WhatsApp cuando se configure el número oficial.
- D-03: etiquetas de HU y estados de prueba fuera del modo normal; flags demo apagados. La limpieza completa de pantallas heredadas se verificará en fases 2–4.
- D-04: la numeración autoritativa es la del backlog adjunto. La matriz histórica `ESTADO_MOVIL.md` contiene numeración diferente y aprobaciones no revalidadas.
- D-05: conservar servicios existentes, con flag previsto apagado; sin desarrollar rutinas nuevas.
- D-06: QR apagado y favoritos disponibles según contrato confirmado. La aplicación efectiva de los flags a los flujos heredados se revisará en fase 4.

## D-07: módulos y paquete

`androidApp` hospeda Activity y Application; `shared` contiene UI, dominio y datos multiplataforma. Paquete `pe.edu.upeu.gymvip`, framework estático `Shared`. Se conserva el vocabulario de las clases de dominio existentes para evitar mezclar una traducción masiva con el cambio de módulos; las incorporaciones se nombran en español.

## D-08: versiones y dependencias

Se adapta el catálogo y los scripts de pharmaMobile: Gradle 9.1.0, AGP 9.0.1, Kotlin 2.4.10, Compose 1.11.1, Ktor 3.6.0, Koin 4.2.2. Material 3 1.11.0-alpha07 y Lifecycle 2.11.0-beta01 provienen de esa referencia: son versiones preliminares, no se presentan como estables. Los iconos usan 1.7.3 como la referencia. Se añade kotlinx-datetime 0.6.2 para conservar la API de fechas existente. SQLDelight pasa de 2.0.2 a 2.1.0 para el nuevo setup y se conserva su caché/cola existentes; no se amplía la sincronización offline.

Se retira la dependencia de Firebase: el repositorio de configuración solo utiliza valores locales, no invocaba el SDK. Android usa CIO y iOS Darwin. La prueba SQLite/JDBC pasa a `androidHostTest` para que no introduzca una dependencia JVM en pruebas iOS. La resolución y compilación se registran en `PRUEBAS.md`.

## D-09: entornos y seguridad

`AppConfig` concentra URLs y flags. Debug Android usa `http://10.0.2.2:8081`; debug iOS `http://localhost:8081`. El mapeo Docker aportado por el usuario es 8081:8081; `application.yml` tiene fallback 8080, por lo que el servidor debe arrancar con PORT=8081 si se ejecuta fuera de esa configuración. Producción conserva la URL HTTPS ya existente; su disponibilidad no se da por confirmada.

HTTP local solo se admite mediante configuración de seguridad de red en debug. Release rechaza cleartext. Se desactiva backup de Android para excluir datos de sesión/caché. El cliente limita Bearer al origen exacto (host, puerto y protocolo) y oculta Authorization y X-Auth-Token en logs; logs apagados en release.

Se conserva el almacén cifrado Android y el almacén iOS existentes; no se los degrada a preferencias simples. La política de fallback y expiración de sesión requiere auditoría en fase 2. No hay endpoint refresh en el AuthController actual: no se añadirá un refresh inventado; se ajustará la expiración a la sesión del servidor.

## D-10: identidad y navegación

Tema oscuro predeterminado, paleta clara existente conservada, colores y formas ajustados al prototipo. Logo GV dibujado en Compose y componente de imagen que admite un Painter de recursos propios/licenciados; no se descargan fotografías externas. Navegación propia con destino guardable y Atrás Android (drawer → pantalla secundaria → pestaña Inicio). Las pantallas secundarias actuales tienen un nivel; no necesitan una biblioteca de navegación adicional.

## Fase 2 — acceso real

Implementada el 9 nov 2026. El contrato real prevalece sobre los supuestos: `email/password` → token/id/email/roles; `member-app/session` indica `mustChangePassword`; `member-app/password` exige 12–72 caracteres, máximo 72 bytes UTF-8, mayúscula/minúscula/número y contraseña distinta. El endpoint revoca todas las sesiones; la app obtiene un token nuevo mediante login antes de consultar el perfil.

Solo se autentica la UI de socio si se recibe rol CLIENTE y un perfil real activo. ADMIN/RECEPCION se rechazan con orientación al portal web y se solicita revocar el token de esa prueba; no se crean interfaces administrativas móviles ni se cambian sus usuarios. Las dos cuentas autorizadas fueron probadas sin incorporar sus contraseñas al repositorio.

Consentimientos: POST y GET de comprobación antes del cambio. El servicio del backend captura errores de SQL al guardar; no basta con confiar en el POST. Se exige que lectura posterior confirme términos, privacidad, WhatsApp y versión `borrador-mobile-v1`. El texto legal es provisional, señalado explícitamente en pantalla.

El cliente lee el token actual en cada petición al origen exacto normalizado. Se elimina el caché Bearer del cliente anterior. Un 401 autenticado cierra la sesión; una respuesta antigua no borra un token nuevo. No hay endpoint refresh y no se simula su existencia. Las contraseñas solo viven temporalmente en campos/operación, fuera de SavedState y del UiState. El ViewModel elimina la contraseña temporal al salir. Onboarding se persiste por dispositivo; el primer cambio pendiente también se persiste y se confirma con el servidor al abrir la app.

Android conserva EncryptedSharedPreferences y elimina el fallback sin cifrar. iOS ahora persiste con NSUserDefaults conforme al MVP del prompt (antes solo tenía memoria); no es Keychain y la migración a Keychain queda pendiente antes de distribuir iOS. No se afirma que iOS haya sido compilado.

Cierre/expiración borra tokens y datos propios de perfil, membresía, pagos, avisos, consentimiento, favoritos y cola. Se descartan cambios offline pendientes al cerrar sesión de acuerdo con la limpieza pedida en el prompt; se reemplaza la política heredada de retenerlos. No se usa un perfil inventado ni una caché como permiso para restaurar una sesión sin validación remota.

La app usa el identificador de acceso entregado por recepción. El backend puede generar un correo técnico de acceso distinto del correo de contacto del cliente; la UI no promete que sean iguales.

## Pendientes explícitos

**Actualización de fase 5 (9 nov 2026):** los pendientes históricos de fase 2 a 4 que siguen en esta sección se cerraron para Android mediante un socio ficticio en la base aislada de QA y recorrido visual en emulador. Primer cambio, consentimiento, Inicio, membresía, pagos, catálogo, favoritos, Avisos y preferencias se verificaron con el backend real. Se probaron las cuentas existentes de ADMIN y RECEPCION sin cambiar sus claves. Se corrigió con autorización el 500 de dos lecturas backend. Resultados: 99 pruebas móviles y 144 backend, sin fallos u omisiones. El backend local final es `gymvip-backend:fase5` en 8081; PostgreSQL conserva datos y usa puerto local 15432. Ver [Fase 5](FASE_5_PULIDO_QA.md). Las líneas que siguen registran el estado histórico al cerrar fase 4.

- Fase 2: prueba de primer cambio real con un usuario CLIENTE temporal autorizado; las cuentas facilitadas son de personal. Modo demo intercambiable pendiente: el acceso implementado usa REST real con `MODO_DEMO=false`.
- Fase 3: implementación y pruebas completadas; pendiente el recorrido CLIENTE positivo contra el servidor y la revisión visual con un dispositivo conectado.
- Fase 4: pantallas, detalles, flags, favoritos/lectura confirmados y preferencias persistentes implementados. Permiso CLIENTE corregido tras autorización y pruebas reales de personal aprobadas. Pendiente revisión visual y recorrido completo con credencial de socio real. Ver `FASE_4_COMUNIDAD.md`.
- Fase 5: pruebas de integración autenticadas contra backend, revisión visual en dispositivo, accesibilidad, firmas y distribución.
- Número oficial WhatsApp pendiente. El valor vacío es intencional y no debe producir un enlace inválido.
- iOS requiere macOS/Xcode para generar y ejecutar el framework y validar el proyecto host.
- La rama personal y publicación se preparan al final conforme a la instrucción directa del usuario. Las capturas y credenciales de QA permanecen ignoradas por Git.

## Fase 4 — contratos reales, caché y roles

Se incorpora `ComunidadRepositoryRest` con modelos comerciales separados de los DTOs y de los modelos heredados del catálogo simple. El flujo nuevo no llama a los seeders de productos/FAQ; se eliminaron sus funciones de relleno. Las rutas, filtros, páginas, precios y estados provienen del contrato real, no de las rutas supuestas del prompt. No se añade una dependencia de imágenes: se consultan miniaturas autenticadas con Ktor y se decodifican con Compose Resources; si fallan se conserva el placeholder propio.

Favoritos y avisos verifican las mutaciones con una lectura posterior. Sus mutex evitan respuestas anteriores sobre cambios nuevos y la comprobación del token antes de HTTP impide enviar una operación antigua bajo la sesión siguiente. La caché privada es por sesión, se borra al salir y no se usa como autorización. No se conecta la cola offline heredada al flujo nuevo.

La ampliación de roles solicitada por el usuario prevalece sobre el alcance original de socio: personal conserva sus paneles, abre consulta comercial y preferencias, y no utiliza rutas de socio para favoritos/avisos/FAQ. La edición comercial no se incorpora a esta fase de consulta. Las preferencias son del dispositivo; no son permisos ni consentimientos del servidor. El texto legal permanece provisional y su consulta no registra aceptaciones nuevas.

Se mantienen dos límites del contrato: una presentación por suplemento y promociones CLIENTE solo vigentes. La discrepancia de permiso global del catálogo se corrigió después de la autorización explícita del usuario, que amplía el alcance permitido del prompt maestro. Se aplicó la propuesta GET concreta de `PROPUESTA_CATALOGO_SOCIO.md` y se mantuvieron escrituras/historiales de ADMIN. No se inventan variantes, porcentajes, promociones finalizadas ni un número de WhatsApp.

Resultado actualizado: APK generado, 95 pruebas móviles aprobadas sin omisiones, incluidas las dos reales de personal contra el backend corregido. Backend: 142 pruebas aprobadas y artefacto probado ejecutándose en la imagen local `gymvip-backend:catalogo-socios`, puerto 8081, salud UP. Se corrigió además el desempaquetado de `published=null` para personal, detectado por integración. No había dispositivo conectado y no se compiló iOS. El recorrido de socio real y el pulido visual siguen siendo requisitos antes de presentar/distribuir.
