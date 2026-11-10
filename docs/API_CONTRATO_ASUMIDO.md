# Contrato REST: contraste con el backend existente

## Conciliación de fase 4

Actualización autorizada del 9 nov 2026: el backend permite las lecturas de catálogo para CLIENTE y conserva las restricciones de personal. Su suite completa aprobó 142 pruebas, incluyendo integración con PostgreSQL aislado y roles de Spring. Se actualizó el backend local y la suite móvil aprobó 95 casos, incluidas las dos cuentas reales de personal. El fallo anterior de Docker queda como registro histórico. Favoritos/avisos/FAQ de CLIENTE siguen sin credencial real de socio facilitada.

| Método / ruta bajo `/api/v1` | Contrato que usa el móvil | Permiso/observación |
|---|---|---|
| GET `/catalog/supplements` | `query`, `category`, `availability`, `minPrice`, `maxPrice`, `withPromotion`, `page`, `size=12`; página `content,totalElements,page,totalPages` | ADMIN/RECEPCION/CLIENTE; para CLIENTE solo publicados |
| GET `/catalog/supplements/{id}` | Suplemento completo, `pricePen`, una `presentation`, `netContent`, `flavor`, `ingredients`, `availability`, `published`, `images[].id` | No se inventan presentaciones múltiples; CLIENTE oculta no publicados |
| GET `/catalog/supplements/{id}/images/{image}?thumbnail=true` | Bytes autenticados; límite móvil de 2 MB y placeholder si falla | No se expone el token en la URL ni se descargan fotos externas |
| GET `/catalog/promotions` | `state=VIGENTE/FINALIZADA`, `supplementId`, `page`, `size`; misma estructura de página | CLIENTE limitado por servicio a vigentes/publicadas |
| GET `/catalog/promotions/{id}` | `supplement`, `name`, `description`, `conditions`, `referencePricePen`, `offerPricePen`, `startDate`, `endDate`, `state` | Precio absoluto en PEN; fechas inclusivas, estado del servidor |
| GET `/member-app/favorites` | Lista con `id,product_id,product_title,product_category,notes,updated_at_utc` | Solo CLIENTE habilitado, aislamiento del backend por cuenta |
| POST `/member-app/favorites` | `{productId,productTitle,productCategory,notes}`; acuse `{id,productId,saved}` | El móvil relee GET y confirma antes de actualizar UI |
| PUT `/member-app/favorites/{id}` | `{notes}`, máximo 250 caracteres | Acuse no contiene el favorito completo; requiere GET posterior |
| DELETE `/member-app/favorites/{id}` | Acuse `{id,deleted}` | Confirmar ausencia mediante GET |
| GET `/member-app/inbox` | Lista `id,accountId,title,message,category,createdAt,isRead` | `accountId` es el id del cliente, no necesariamente id de login |
| POST `/member-app/inbox/{id}/read` | Acuse, seguido de GET para verificar `isRead` | No es PATCH; el servicio puede capturar errores SQL y devolver acuse sin persistencia |
| GET `/member-app/faqs` | Lista `id,question,answer,category,orderIndex` | Solo CLIENTE; buscar/agrupar localmente sin rellenar datos |

Ejemplo de lectura de favorito (valores ilustrativos, no un usuario predefinido):

```json
[{"id":"favorito-id","product_id":"suplemento-id","product_title":"Producto publicado","product_category":"PROTEINA","notes":"Mi nota","updated_at_utc":"2026-11-09T12:00:00"}]
```

Los errores de permisos se muestran como tales; no se traducen a listas vacías ni a productos demo. Las cuentas de personal no consumen favoritos, FAQ ni avisos de member-app. No se exige membresía vigente en la app para las consultas de socio. Se mantienen las restricciones de primer cambio de contraseña y rol del backend.

`GET /catalog/options` declara `whatsappConfigured=false` y categorías PROTEINA/CREATINA/PREENTRENO/AMINOACIDOS/VITAMINAS/OTROS. La búsqueda `query` incluye marca: no se usa un parámetro independiente `brand` que el servidor no tiene. El número oficial sigue vacío en `AppConfig`.

La discrepancia se corrigió tras autorización explícita del usuario; véase `PROPUESTA_CATALOGO_SOCIO.md` y `gymvip-backend/docs/CATALOGO_SOCIOS.md`. ADMIN sigue siendo el único rol de escritura comercial en el controlador; la fase 4 móvil añade consulta, no formularios de edición comercial. El parámetro `published` opcional de personal conserva null sin error 500; para CLIENTE se fuerza true.

## Conciliación de fase 3

Inicio, Membresía e Historial de pagos usan el workspace único GET `/api/v1/member-app/me`: `client`, `coverage`, `periods`, `payments`. Se validan `client.id` y `coverage.clientId` contra el socio autenticado. Las rutas `/api/v1/me/membership` y `/api/v1/me/payments` no existen y sus fallbacks fueron eliminados.

`coverage.state` devuelve VIGENTE/POR_VENCER/VENCIDO/CANCELADA/PENDIENTE/SIN_MEMBRESIA. `lastIncludedDate` ya es inclusiva; `endExclusive` y `periods[].end_date` son exclusivas. `daysRemaining` se respeta si se recibe. `periods[]` aporta `duration_days`, `benefits`, `conditions`, `paid` y `cancelled_at_utc`. `payments[]` aporta `plan_name`, `amount_pen`, `payment_method`, `external_reference`, `notes` y `payment_timestamp_utc`. No se supone que el servidor entregue conciliación ni reemplazo de pagos si esos campos no están.

Contrato cotejado con una respuesta real bajo lectura autorizada de personal en `/members/{id}`. Pruebas directas confirman que ADMIN/RECEPCION no pueden acceder a `/member-app/me`; no se relajaron esos permisos. El recorrido positivo con token CLIENTE sigue pendiente de una cuenta de socio. Detalle en `FASE_3_SOCIO.md`.

La ampliación de acceso móvil a ADMIN/RECEPCION reutiliza los contratos existentes de personal. La matriz de rutas, permisos y pruebas está en `ACCESO_POR_ROLES.md`. Las evidencias de rechazo de personal de fase 2 son históricas y fueron reemplazadas por pruebas de acceso autorizado y denegaciones específicas.

El backend no está vacío. Las rutas españolas `/socios/me`, `/avisos` y `/suplementos` del prompt eran supuestos; se conservarán los adaptadores para las rutas del backend real. Confirmado aquí significa inspeccionado en código, no probado en una sesión autenticada.

| Método / ruta | Evidencia | Estado |
|---|---|---|
| POST `/api/v1/auth/login` | AuthController | Confirmado en código |
| POST `/api/v1/auth/logout` | AuthController | Confirmado en código |
| GET `/api/v1/auth/me` | AuthController | Confirmado en código |
| GET `/api/v1/member-app/session` | MemberAppController: mustChangePassword | Confirmado en código |
| POST `/api/v1/member-app/password` | MemberAppController: changed, requiresLogin | Confirmado en código |
| GET `/api/v1/member-app/me` | MemberAppController: workspace del socio | Confirmado en código |
| GET `/api/v1/member-app/attendance` | MemberAppController | Confirmado en código |
| GET `/api/v1/member-app/qr` | MemberAppController | Confirmado en código; flag apagado |
| GET/POST `/api/v1/member-app/favorites` | MemberAppController | Confirmado en código |
| PUT/DELETE `/api/v1/member-app/favorites/{id}` | MemberAppController | Confirmado en código |
| GET `/api/v1/member-app/faqs` | MemberAppController | Confirmado en código |
| GET `/api/v1/member-app/services` | MemberAppController | Confirmado en código |
| GET `/api/v1/member-app/inbox` | MemberAppController | Confirmado en código |
| POST `/api/v1/member-app/inbox/{id}/read` | MemberAppController | Confirmado en código |
| GET/POST `/api/v1/member-app/consents` | MemberAppController | Confirmado en código |
| POST `/api/v1/auth/refresh` | No está en AuthController | No disponible |
| POST `/api/v1/auth/activate` | Ausente en AuthController | Eliminado del adaptador móvil en fase 2 |

MemberAppController requiere rol CLIENTE y restringe las operaciones mientras mustChangePassword sea verdadero. El cambio de contraseña devuelve requiresLogin=true: no asumir continuación con el token anterior.

## Acceso conciliado en fase 2

Login: petición `{ "email": "socio@example.test", "password": "<credencial ingresada>" }`; respuesta `{ "token": "<token>", "id": "<account UUID>", "email": "socio@example.test", "roles": ["CLIENTE"] }`. No devuelve refreshToken ni el indicador de primer cambio.

Sesión: GET `/member-app/session` devuelve `{ "mustChangePassword": true }`. Primero se consulta esta ruta; no se usa el perfil protegido como comprobación de primer ingreso.

Cambio: POST `/member-app/password` con `{ "currentPassword": "<temporal>", "newPassword": "<nueva>" }`; respuesta `{ "changed": true, "requiresLogin": true }`. Reglas reales: mínimo 12 caracteres, máximo 72 caracteres y bytes UTF-8, mayúsculas/minúsculas ASCII, número y diferente de la actual. Se necesita un login nuevo después del cambio.

Consentimiento: POST `/member-app/consents` con `{ "termsAccepted": true, "privacyAcknowledged": true, "whatsappConsent": true, "documentVersion": "borrador-mobile-v1" }`, seguido de GET y comprobación de los mismos campos. No se envían consentimientos inventados dentro del payload de contraseña.

El perfil usa `client.id` como clave local; el id de login identifica la cuenta y puede ser diferente. Se conserva el correo de acceso de login para renovar la sesión después del cambio, aunque el correo de contacto del perfil difiera.

Probado contra el servidor local: autenticación ADMIN/RECEPCION, rechazo de esos roles por AuthRepositoryImpl y solicitud de cierre de sus sesiones. El primer cambio CLIENTE y la consulta de perfil se probaron con respuestas del contrato y MockEngine/SQLite; pendiente de una cuenta real autorizada para completar ese recorrido.

Pendiente para fases 3–5: contrastar los DTOs de membresía, pagos, catálogo y demás recursos con respuestas reales del socio. No se inventan evidencias de endpoints no probados.
