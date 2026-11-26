# Revisión y correcciones — Mi Bitácora / Catálogo

Fecha: 26 de noviembre de 2026. Rama: feature/bitacora-entrenamiento.

## Fallos corregidos

1. Cachés de Entreno: un JSON dañado de sesiones o rutinas impedía abrir los registros manuales. Ahora la lectura es transaccional, mantiene los registros disponibles y muestra un aviso sobre la caché que no pudo leer. No se sobrescriben datos dañados ni se modifica Entreno. Si el JSON de la propia bitácora está dañado, se informa del error y se protege de sobrescrituras.
2. Exportación de imágenes: un cambio reciente de formato o privacidad podía dejar temporalmente disponible la imagen anterior. El botón requiere que configuración, PNG generado y vista previa decodificada correspondan entre sí. La decodificación sale del hilo principal y las cancelaciones se propagan.
3. Texto largo: la tarjeta calculaba el ajuste por aproximación y podía recortar contenido. Ahora mide cada línea real, ajusta tipografía y rechaza explícitamente un contenido que no cabe, con opción de reintento y de reducirlo.
4. Cronómetro: se conservan los milisegundos entre pausas, se calcula el tiempo exacto al detener, se explica el redondeo al siguiente minuto y se permite reiniciar. La hora inicial toma la del dispositivo; teclados numéricos/decimales y validación de identificadores consistentes.
5. Guardado: se bloquea salir/cancelar durante la escritura y se libera el botón antes de esperar el Snackbar. Se rechazan identificadores de sincronización repetidos sin perder el registro anterior. El estado de diálogo y navegación de la bitácora se reinicia por cuenta.
6. Archivos de imagen: la limpieza de exportaciones no borra una foto de cámara todavía en uso; las imágenes de fondo se liberan incluso si falla el dibujo.
7. Promociones: se incorpora prioridad a la selección de ofertas. Desempate por precio e identificador, para alinear la elección con la regla del carrito. Compatibilidad con respuestas antiguas sin priority (valor cero).
8. Firma de producción: validación sobre el grafo real de tareas, incluyendo assemble/build agregados. Debug no exige keystore de producción; release no cae a firma debug.

## Backend local compatible

La imagen que estaba ejecutándose (fase5) no incluía las rutas de Entreno que usa la rama móvil actual. Se compiló el código existente de gymvip-backend origin/develop en el commit 087a044 en una copia separada, sin editar el checkout anterior.

Antes del cambio se guardó un pg_dump y se restauró en la base separada gymvip_compatibilidad_quispe, dentro del mismo PostgreSQL. Flyway aplicó las migraciones existentes V13–V31 sobre esa copia. La base original gymvip_sprint1_jorge permanece como respaldo; no se repararon checksums ni se editaron migraciones.

- Backend activo: gymvip-backend-s3-mobiles-v2, imagen gymvip-backend:bitacora-compatible, puerto 8081, estado healthy, límite 1 GB/2 CPU.
- PostgreSQL: el mismo contenedor gymvip-sprint1-jorge-db. Conserva las dos bases; no hay dos motores PostgreSQL nuevos.
- Backend anterior: gymvip-backend-respaldo-quispe, detenido, restart=no. No consume CPU/memoria como proceso activo y no vuelve a ocupar el puerto al reiniciar Docker.
- Para compilar desde Windows se normalizó CRLF del wrapper en un Dockerfile auxiliar y se usó Maven Central como repositorio de compilación. No se cambiaron versiones del backend ni se añadieron endpoints propios.
- Cuenta de revisión: la cuenta CLIENTE ficticia ya existente. No se cambiaron contraseñas de administrador o recepción.

Respaldos, configuración privada y logs quedan fuera de Git, en la carpeta local gymvip-bitacora-evidencias. El Dockerfile auxiliar de ejecución no contiene secretos. El backend original con sus cambios locales sigue intacto.

## Comprobaciones

- desktopTest: 85 pruebas, 0 fallos, 0 errores.
- testDebugUnitTest: las mismas 85 pruebas comunes en el target Android, 0 fallos, 0 errores.
- assembleDebug / assembleDebugAndroidTest: BUILD SUCCESSFUL.
- Instrumentación Android de imágenes: tres comprobaciones reales (seis PNG de plantillas/formatos, rechazo explícito de contenido excesivo y cancelación sin bloquear la siguiente imagen). Preparación de sesión opcional omitida cuando no se solicita.
- assemble --dry-run sin credenciales de producción: bloqueado como se esperaba por la validación de release. No es un fallo del build debug.
- Socio real del backend local: me, inbox, rutinas, ejercicios, progreso, catálogo, promociones y sedes: HTTP 200.
- ADMIN /accounts: 200. RECEPCION /accounts: 403 esperado por permisos.
- Creación de sesión con timestamps UTC terminados en Z: correcta. Reenvío del mismo clientSyncId: deduplicated=true. Se eliminó únicamente esa sesión ficticia temporal al finalizar.
- Integridad: coincidencia de conteos y huella del contenido original de clients, memberships, membership_periods, payment_records, supplements, supplement_images y supplement_promotions entre la base original y la copia migrada.
- Datos conservados en la copia: 9 suplementos. Entreno disponible con 2 rutinas y 8 ejercicios de la migración oficial.
- Verificación visual en el emulador: Entreno carga sin el error anterior y la vista previa de publicación 1:1 muestra todos los datos del día de prueba.

## Límites

La sincronización remota de la bitácora sigue fuera del alcance de esta fase local. iOS requiere macOS para validar su compilación; no se afirma que se haya ejecutado aquí. No se publicaron imágenes en redes ni se enviaron mensajes a terceros.
