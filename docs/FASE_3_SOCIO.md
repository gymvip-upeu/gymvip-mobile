# Fase 3 — Inicio, membresía e historial de pagos

9 nov 2026, America/Lima.

## Entrega

Las tres pantallas del socio consumen el workspace real de GET `/api/v1/member-app/me`. `SocioViewModel` comparte una única carga entre Inicio, Membresía y Pagos. Se mantienen el panel administrativo, el de recepción y la matriz de permisos de `ACCESO_POR_ROLES.md`.

- **Inicio:** saludo según Lima, nombre real, campana y contador de avisos, estado de actualización con tiempo transcurrido, tarjeta de membresía y accesos a Mis pagos, Avisos y Catálogo. Se elimina el aviso de feriado inventado del fallback de configuración. No se simulan promociones; la sección Para ti se incorporará con la integración comercial de fase 4.
- **Membresía:** estado y fechas del servidor, duración del plan cuando está disponible, condición de pago presencial, condiciones/beneficios expandibles y acceso al historial de pagos. Si hay varios periodos se muestran sus fechas, importes, pago y cancelación. Se llama Historial de periodos porque el servidor no distingue alta y renovación en ese payload.
- **Pagos:** pantalla propia accesible desde Inicio y Membresía, con Volver y botón Atrás conservando el origen. Orden cronológico descendente, importes PEN, fecha/hora Lima, medio, plan, operación y nota si existen. Transferencia se reconoce expresamente. No se muestra el antiguo badge CONCILIADO sin evidencia del backend. Reemplazado solo aparece si ese estado viene en la respuesta; el contrato actual no lo envía.
- **Interacción:** pull-to-refresh, esqueleto inicial, estados vacíos específicos, error con Reintentar y contenido anterior cuando corresponde. El aviso de vencimiento se muestra una vez por sesión, no bloquea la consulta y dirige a renovación presencial. WhatsApp permanece oculto si no hay número oficial configurado.

Se ajustaron colores, radios, separación, tarjeta dorada/oscura, iconos y composición a los prototipos. No se descargaron imágenes ajenas ni se mostraron etiquetas de desarrollo. Los encabezados duplicados se retiraron de las tres vistas del socio.

## Contrato y fechas

- Se corrige el mapeo de `POR_VENCER` y `VENCIDO` del servidor. Se reconocen también CANCELADA, PENDIENTE y SIN_MEMBRESIA. Un estado desconocido se muestra como no disponible, nunca como vigente.
- `coverage.state` y `daysRemaining` mandan. Solo cuando falta el contador se deriva desde `endExclusive` en Lima.
- `lastIncludedDate` es el último día válido. Si no está, se obtiene restando un día al fin exclusivo; el mismo ajuste se hace para el historial de periodos.
- La cobertura puede unir renovaciones contiguas. Los beneficios, condiciones y duración del plan se toman de un periodo pagado/no cancelado cuyo fin coincide con el bloque seleccionado. No se toma sin más el primer periodo, que podría ser futuro o no pagado.
- Sin cobertura no se inventa una membresía a partir de periodos no pagados. Los pagos siguen accesibles con membresía vencida o sin membresía vigente.
- Las fechas de pago sin offset se interpretan como UTC, tal como las produce `MemberWorkspaceService`, y se presentan en America/Lima.

## Caché, sesión y roles

`MemberRepositoryImpl.refreshSummary` obtiene una sola respuesta y actualiza perfil, membresía y pagos en una transacción local. La instantánea completa y su marca de actualización se conservan en memoria de la sesión, conforme al alcance de caché del prompt. No se cambió el esquema de SQLite. Los registros SQLite existentes se siguen actualizando para compatibilidad; no se los utiliza para eludir la revalidación de sesión al reiniciar.

Antes de pedir datos y después de recibirlos se comprueban la sesión CLIENTE, su identidad y el token. El `client.id` y, cuando viene, `coverage.clientId` deben coincidir con la cuenta del socio autenticado. Una respuesta tardía después de logout no repuebla la caché; una respuesta de un token viejo tampoco borra un login nuevo del mismo socio. Los datos desaparecen al cerrar/expirar la sesión. Un 403 del propio núcleo de socio obliga a reingresar.

ADMIN y RECEPCION mantienen sus rutas de personal. El repositorio de socio no permite que esos roles ni una cuenta ajena consulten este núcleo desde la UI. El backend sigue aplicando la autorización final. No se modificaron endpoints ni permisos del servidor.

Una caída de red conserva la instantánea disponible y muestra Sin conexión. Un error 5xx también puede conservar datos anteriores, pero se presenta como fallo de actualización, sin etiquetarlo falsamente como falta de red. El banner describe membresía/pagos, no presume que todo el catálogo o las notificaciones estén sincronizados.

## Verificación ejecutada

```powershell
.\gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --offline --console=plain
```

Resultado final con las variables de las pruebas reales habilitadas: **BUILD SUCCESSFUL en 20 s; 66 pruebas, 0 fallos y 0 omitidas**.

Se conservan las 45 pruebas previas y se añaden:

| Suite | Pruebas | Cobertura |
|---|---:|---|
| ResumenSocioMapperTest | 9 | Estados, fin exclusivo, último día Lima, días del servidor, selección de periodo, pagos/transferencia, orden y fechas, cancelación, propietario |
| NucleoSocioRepositoryTest | 9 | Carga única, caché, vacíos, 401, roles, cuenta ajena, logout con respuesta tardía, token nuevo y aviso una vez por sesión |
| SocioViewModelTest | 3 | Carga sin duplicados, vacío válido, red frente a servidor, recuperación y expiración sin datos en UI |

Las dos pruebas reales de personal se volvieron a ejecutar con las cuentas autorizadas: login/restauración, lecturas permitidas y Forbidden de cuentas para recepción. Se añadió la comprobación de que ADMIN/RECEPCION reciben Forbidden en `/member-app/me`. Se cotejó además un workspace real a través de la ruta de lectura autorizada de administrador `/members/{id}`: perfil, cobertura y propietario consistentes, sin imprimir datos personales ni modificar registros. Los tokens de prueba se cerraron.

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Reporte HTML: `shared/build/reports/tests/testAndroidHostTest/index.html`.

## Límites de esta entrega

- No hay una cuenta CLIENTE proporcionada para el recorrido positivo completo de socio en el servidor. El acceso y la transformación de `/member-app/me` se prueban con MockEngine y SQLite; el esquema de workspace sí se contrastó con una respuesta real bajo permisos de administrador. Esto no equivale a haber ingresado como socio real.
- No había emulador ni teléfono conectado durante esta fase; falta comprobar visualmente los nuevos recorridos en dispositivo. iOS requiere macOS/Xcode.
- Promociones de Inicio y las pantallas comerciales/comunicación quedan para fase 4. No se muestra una promoción inventada ni se amplían permisos del backend para obtenerla.
- No se hacen pagos, renovaciones ni cambios contractuales desde estas pantallas de consulta.

## Archivos principales

`ResumenSocio.kt`, `FechasSocio.kt`, `ResumenSocioMapper.kt`, `MemberRepositoryImpl.kt`, `SesionLocal.kt`, `SocioViewModel.kt`, `HomeScreen.kt`, `MembershipScreen.kt`, `PaymentsScreen.kt`, `ComponentesSocio.kt`, `VipCards.kt` y `App.kt`.
