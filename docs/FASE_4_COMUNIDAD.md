# Fase 4 — comercial y comunicación

Registro de la fase 4, 9 de noviembre de 2026. Usa REST real con `MODO_DEMO=false`. Después de la autorización del usuario se corrigió el permiso CLIENTE en el backend. El resultado de esta fase fue 142 pruebas backend y 95 móviles aprobadas, incluidas las dos cuentas reales de personal. Los pendientes de recorrido de socio y revisión visual se abordaron en la [fase 5](FASE_5_PULIDO_QA.md), con datos de socio ficticios y base aislada; los resultados actuales son 144 backend y 99 móviles.

## Entrega

- Catálogo en dos columnas: búsqueda por nombre/marca, categorías del contrato, precio mínimo/máximo, disponibilidad, promociones y paginación. Imágenes autenticadas del producto con placeholder local si faltan.
- Precios regulares y promociones vigentes confirmadas por producto. Se consultan solo los productos de la página visible, con un máximo de cuatro solicitudes de promociones simultáneas. Un fallo conserva el precio regular y avisa; no calcula descuentos ficticios.
- Detalle de producto con su presentación real, contenido, sabor, ingredientes, disponibilidad y promoción. El backend tiene una sola presentación por producto: no se inventa un selector de variantes.
- Promociones vigentes/finalizadas y filtro desde el detalle de un producto; condiciones, fechas inclusivas, precio de referencia/oferta y disponibilidad. Se respeta el estado calculado por el servidor.
- Favoritos de socio detrás de `FEATURE_FAVORITOS`: guardar, quitar, editar nota de hasta 250 caracteres y confirmar cada escritura releyendo el servidor.
- Avisos: lista, detalle, lectura y contador compartido en Inicio/barra inferior. Un POST sin persistencia confirmada no cambia el contador. Se valida el propietario recibido.
- Ayuda y FAQ reales, buscador, secciones gimnasio/productos y acordeón. No se insertan FAQ ni productos de demostración.
- Perfil con nombre/código reales y contactos enmascarados, confirmación de cierre, acceso a ayuda/preferencias y QR informativo. No permite editar datos personales.
- Preferencias persistentes por dispositivo: oscuro/claro/sistema y avisos emergentes de vencimiento; textos legales provisionales solo de consulta. Versión tomada del build. El cambio de tema también actualiza los iconos de barras del sistema Android.
- Conexión WhatsApp preparada con mensaje codificado. `WHATSAPP_GYM` sigue vacío: se indica consultar en recepción y no se genera un enlace inválido.

Se mantienen los colores, tarjetas, barra inferior y jerarquía de los prototipos. No se afirma que la fidelidad visual haya sido verificada en esta fase: ADB no encontró dispositivos conectados.

## Roles y sesiones

| Función nueva | ADMIN | RECEPCION | CLIENTE |
|---|---|---|---|
| Consulta de catálogo/detalles/promociones | Sí | Sí | Sí, publicados/vigentes; permiso corregido tras autorización |
| Favoritos propios, avisos, FAQ de member-app | No | No | Sí, con token CLIENTE habilitado |
| Preferencias locales | Sí | Sí | Sí |
| Edición comercial | No se añade a esta fase móvil | No | No |

Los paneles de personal conservan la gestión previa de socios, planes y cuentas según su rol. Catálogo y preferencias se abren desde Inicio/Perfil del personal sin convertirlo en socio. No se solicita `/member-app/me` ni se crean favoritos/avisos de socio para personal.

La caché comercial es de esta sesión y recurso; las listas privadas viven en `SesionLocal` y se borran al cerrar/expirar. Un fallo conserva datos cargados para la misma consulta y muestra error/reintento; sin red con datos previos se muestra un aviso ámbar. Cambiar filtros no conserva resultados de otra consulta. No hay cola offline de escrituras en el flujo nuevo.

Favoritos y avisos serializan sus lecturas/escrituras para evitar que una respuesta anterior revierta un cambio confirmado. Cada operación conserva el token de origen, incluso al esperar un mutex; el interceptor vuelve a comprobarlo antes de enviar y los commits lo comprueban después. Una sesión nueva no hereda operaciones ni datos de la anterior. Cancelar una coroutine sigue propagando `CancellationException`.

## Contratos y límites del servidor

1. Resuelto tras autorización del usuario: `SecurityConfig` permite las rutas GET concretas para CLIENTE conservando las restricciones del controlador. No se abrieron escrituras, historiales ni otras rutas administrativas. Se corrigió también el filtro opcional `published` nulo que producía 500 para personal. Ver `PROPUESTA_CATALOGO_SOCIO.md` y `gymvip-backend/docs/CATALOGO_SOCIOS.md`.
2. El servicio de promociones limita CLIENTE a promociones vigentes publicadas aun si se solicita `FINALIZADA`. La app explica ese límite y filtra la respuesta: nunca presenta una vigente como finalizada. ADMIN/RECEPCION pueden consultar finalizadas según su contrato.
3. FAQ y avisos se reciben completos; no se fabrican nombres, fechas de vencimiento ni texto adicional. El contenido depende del backend.
4. Falta un número oficial de WhatsApp. QR permanece informativo y servicios oculto por sus flags.
5. No se añaden checkout, pagos online, renovaciones, notificaciones push ni cambios de permisos del servidor.

## Verificación

Comando de compilación y pruebas locales:

```powershell
.\gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain
```

Reporte actualizado en `PRUEBAS.md`; APK en `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

Se ampliaron las dos pruebas optativas `BackendRealAuthTest` para consultar catálogo/promociones con las cuentas de personal autorizadas y comprobar denegación de rutas privadas. No realizan escrituras comerciales ni de socios. El primer intento falló por indisponibilidad de Docker/localhost:8081. Tras recuperar los servicios y actualizar el backend, las dos pruebas reales y la suite móvil completa pasaron: 95 casos, sin fallos ni omitidos, en 22 s. No se restablecieron datos ni se modificó la instalación de Docker. El contenedor previo quedó detenido para restauración; el actual usa `gymvip-backend:catalogo-socios` y conserva puerto, red y variables.

Las cuentas entregadas son de ADMIN y RECEPCION; no existe una credencial CLIENTE facilitada para probar positivamente sus favoritos, avisos y FAQ. Esas rutas se verifican con el contrato real y MockEngine/SQLite, no con una sesión de socio real. iOS requiere macOS/Xcode; no se compiló en Windows.

## Archivos clave

- `domain/model/Comunidad.kt`, `domain/repository/ComunidadRepository.kt`.
- `data/remote/api/ComunidadApi.kt`, `data/remote/dto/ComunidadDtos.kt`, `data/repository/ComunidadRepositoryRest.kt`.
- `presentation/viewmodel/ComunidadViewModel.kt`.
- `CatalogoComunidadScreen.kt`, `DetalleComercial.kt`, `AvisosComunidadScreen.kt`, `AyudaComunidadScreen.kt`, `ProfileScreen.kt`, `PreferencesScreen.kt`.
- `ComunidadRepositoryTest.kt`, `ComunidadViewModelTest.kt`, `PreferenciasComunidadTest.kt`, ampliación de `BackendRealAuthTest.kt`.

## Recorrido manual pendiente

- ADMIN/RECEPCION: login → Inicio → catálogo → buscar/paginar → detalle → promociones/finalizadas → volver al panel → preferencias → cerrar sesión. Recepción sigue sin cuentas ni edición de planes.
- CLIENTE autorizado: login → Inicio → catálogo → detalle → favorito/nota/quitar → aviso/detalle/contador → perfil → tema/avisos → FAQ → cerrar sesión → otra cuenta sin datos cruzados. El permiso global ya está resuelto; falta la credencial real de socio y el dispositivo para este recorrido.
- Repetir con membresía vigente/vencida/sin membresía, sin red, errores, listas vacías, rotación, Atrás y texto grande. La consulta comercial no depende del estado de la membresía.
