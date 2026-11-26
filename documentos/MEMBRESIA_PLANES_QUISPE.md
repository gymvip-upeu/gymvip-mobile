# Membresía y acceso a Mi Bitácora

26 de noviembre de 2026. Rama `feature/bitacora-entrenamiento`.

El botón «Ver Planes y Precios Disponibles» abre una hoja modal en Membresía. No cambia a Tienda. Consulta `GET /api/v1/member-app/plans` con la sesión CLIENTE y muestra nombre, duración, precio PEN, descripción, beneficios y condiciones publicados. Los cuatro planes actuales son mensual (30 días, S/120), trimestral (90 días, S/320), semestral (180 días, S/580) y anual (365 días, S/990). No se inventaron promociones ni contratos.

La hoja permite cerrar, reintentar y recorrer listas largas; incluye estados de carga, error y lista vacía. Ante fallos de red/servidor puede mostrar una copia guardada por cuenta, con advertencia de confirmar precios en recepción. Un 401/403 no usa esa copia para esconder el error. Una respuesta vacía sustituye la lista anterior.

La tarjeta del contrato muestra el estado autoritativo, nombre, días restantes, inicio, último día incluido y duración. Los detalles comerciales se toman del período que corresponde a las fechas de cobertura; no del primer elemento del historial. Los períodos usan fin exclusivo; la fecha visible conserva `lastIncludedDate`. Una cobertura que reúne períodos no hereda condiciones de otro período ni de uno cancelado. La duración de cobertura se calcula incluyendo el último día válido cuando no existe un período exacto. Los detalles se conservan en preferencias por cuenta sin modificar el esquema SQLDelight. El medio de pago aparece solo si hay un pago vinculado a ese período.

Mi Bitácora continúa en **Entreno → Mi Bitácora · registrar mi día**, y en el menú lateral. La antigua carpeta abierta en Android Studio era `gymvip-mobile-wagner`, en `codex/wagner-admin-preview`, anterior a este módulo. Se conserva esa carpeta y se actualiza a la rama integrada; sus ajustes locales anteriores quedan respaldados en Git. No se crea una segunda aplicación.

Backend: nueva lectura comercial exclusiva CLIENTE; no altera `/plans`, ventas, pagos, permisos administrativos ni esquema. Docker conserva un backend activo en 8081 y el PostgreSQL existente. Los contenedores de respaldo quedan detenidos con `restart=no`.

Validación: pruebas JVM Android/escritorio de endpoint, copia por cuenta, 401/403, lista vacía, contrato correcto, fechas y duración; prueba backend de filtro ACTIVO; integración real CLIENTE 200 en la nueva consulta, CLIENTE 403 en lectura y escritura administrativa, ADMIN 200 en gestión. Recorrido visual del socio local sin membresía y captura de contrato ficticio para comprobar el diseño sin modificar sus datos.

## Cierre de verificación

- Carpeta activa de Android Studio: `H:/ProyectosUPEU/gymvip-mobile-wagner`, misma ventana y repositorio; rama `feature/bitacora-entrenamiento`. Ajustes anteriores: stash `respaldo-configuracion-local-antes-integrar-bitacora-2026-11-26`.
- Compilación desde esa carpeta: `desktopTest testDebugUnitTest assembleDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL; 90 pruebas por suite, cero fallos/errores/omitidas. Prueba de tarjeta en dispositivo: 1 aprobada, con contrato ficticio y captura PixelCopy de la superficie de la app. No se registró una membresía real para obtener la imagen.
- Las pruebas que usan JDBC se trasladan a `jvmTest`, compartido por escritorio y Android host. La importación de iOS ya no intenta resolver el driver JDBC como biblioteca nativa.
- La URL local de debug se lee de `local.properties` ignorado. No se publica una ruta de JDK ni una dirección local obligatoria para el equipo.
- Se corrige la validación de firma: importar modelos/recursos release desde Android Studio no exige keystore; producir artefactos release sí. `prepareKotlinIdeaImport` termina correctamente; `assembleRelease --dry-run` sin credenciales sigue rechazado.
- Backend publicado en `JorgeQuispe/planes-membresia-socio`, commit bb7f040. La nueva consulta requiere incorporar ese cambio al backend de otros entornos.
- APK debug actualizado en `C:/Users/joorg/Downloads/GymVIP-Bitacora-Quispe.apk`. Este APK usa el Docker local desde el emulador; no convierte la PC en un servidor accesible desde teléfonos externos.

Limitación heredada observada: el emulador usa páginas de 16 KB y muestra un aviso de modo de compatibilidad para las bibliotecas nativas del escáner/cámara (`libbarhopper_v3.so`, `libimage_processing_util_jni.so`). La app funciona tras aceptar el aviso; no se afirma compatibilidad nativa de 16 KB ni se ocultó el aviso como si fuera una corrección. Su actualización requiere una revisión separada de esas dependencias. Se mantienen además las advertencias de deprecación ya existentes de Gradle/almacenamiento seguro. La compilación iOS y la firma de producción no se acreditan en esta revisión Windows.
