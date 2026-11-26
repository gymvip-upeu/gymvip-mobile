# Mi Bitácora y Compartir mi día — Jorge Quispe

Rama: `feature/bitacora-entrenamiento`, creada desde `origin/develop` (`5f95e1c`). Incluye también los últimos cambios locales del catálogo de Quispe (paginación, imágenes, promociones y pestañas).

## Arquitectura

Código nuevo en `com.gymvip.mobile.features.journal` en commonMain, androidMain, iosMain y desktopMain. DI mediante Koin y AppContainer. Entrada por el menú del socio y un botón de Entreno. La ruta exige una sesión CLIENTE. No cambia la lista de pestañas ni asigna permisos.

`JournalRepository` usa `local_preference`, clave `journal_entries_v1_json`, con `accountId` obligatorio. CRUD transaccional, JSON serializable, identificadores estables al editar y ninguna llamada al backend. `JournalSyncTransport` reserva el contrato de sincronización futura; no implementa ni consume un endpoint nuevo. `GymVipDatabase.sq` queda intacto.

Las sesiones se leen de `local_workout_sessions_json`; las rutinas vinculables de `cache_routines_json`. La bitácora no llama a fetchProgress ni syncPendingWorkouts y no modifica las colas, sesiones o rutinas. Una anotación manual representa actividad adicional: no debe volver a introducirse como duración de una sesión que ya aparece en el detalle.

Las sesiones sincronizadas existentes solo conservan un resumen local (fecha, duración y volumen). No se inventan series ni récords por ejercicio. Las curvas Epley usan los ejercicios manuales disponibles. Las fechas instantáneas se convierten a la zona del dispositivo; las fechas locales de Entreno mantienen su día. America/Lima es el respaldo de zona.

## Funciones

- Calendario mensual, detalle diario, racha, días entrenados, tiempo y promedio por día entrenado.
- Formulario con fecha y hora, duración o cronómetro, tipo, músculos, energía/esfuerzo, filas de ejercicios, cuatro textos y rutina opcional.
- Crear, editar y eliminar registros manuales; las sesiones existentes son de solo lectura.
- Evolución con Canvas, estimación Epley, tiempo y volumen por semana y comparativa mensual. Las cifras también se presentan como texto.
- Tres plantillas, formatos 1080x1920 / 1080x1080, fondo vectorial propio o foto, controles de contenido y nombre desactivado inicialmente.
- La vista previa Android muestra el PNG real que se guarda/compartirá. Rasterización con Canvas nativo: el proyecto usa Compose 1.6.11, no la versión 1.7.1 mencionada en el prompt.
- Android Photo Picker sin permisos de almacenamiento, cámara externa con el flujo de permiso existente, orientación EXIF y límite de decodificación para fotos grandes.
- MediaStore en Pictures/GymVIP desde Android 10; en Android 7–9 se ofrece el selector de compartir/guardar sin solicitar acceso amplio al almacenamiento.
- ACTION_SEND con permisos de lectura temporales y FileProvider limitado a cache/journal. Limpieza de temporales antiguos.
- iOS y desktop proporcionan servicios seguros con mensaje de función no disponible para exportación. El registro local sí es común.

## Excepciones justificadas al listado de archivos de integración

1. `AndroidManifest.xml`: FileProvider requerido por el propio prompt; recurso nuevo `journal_file_paths.xml`.
2. `composeApp/build.gradle.kts`: la configuración previa impedía incluso compilar debug sin keystore de producción. Solo se difiere esa exigencia a las tareas release; no hay firma de producción con clave debug ni cambios de dependencias.
3. `AuthAndErrorDtos.kt`: se elimina el prefijo estándar ROLE_ al restaurar roles de /auth/me. Sin esto una respuesta ROLE_CLIENTE hacía desaparecer Entreno tras reiniciar. Se conserva exactamente el permiso asignado por el servidor. Prueba de regresión para CLIENTE y ADMIN.
4. Archivos de catálogo: traslado autorizado de los cambios que ya estaban en la copia anterior; pruebas específicas incluidas.

## Cómo probar

1. Abrir esta copia del proyecto (gymvip-mobile-bitacora), usar JDK 21 y un emulador Android.
2. Para backend Docker local: compilar con `-Pgymvip.backendUrl=http://10.0.2.2:8081`. No requiere cambiar la URL de release.
3. Ingresar con una cuenta cuyo rol real sea CLIENTE; administrador/recepción no deben ver Mi Bitácora.
4. Menú > Mi Bitácora o Entreno > Mi Bitácora. Crear un registro con fecha de hoy y duración positiva. Verificar calendario y detalle; editarlo y comprobar que no se duplique.
5. Registrar ejercicios en varios días para ver puntos Epley y barras semanales. Abrir un mes anterior para comparar estadísticas.
6. Desactivar la conectividad y verificar que registrar, editar, consultar y borrar siguen funcionando. Restablecerla al terminar.
7. En el detalle, Compartir mi día. Probar plantillas, formatos y controles. El nombre empieza desactivado. Revisar foto propia, cancelar galería, permitir/denegar cámara, guardar imagen y abrir el selector de compartir.
8. Reiniciar la app y comprobar persistencia y acceso CLIENTE. Cambiar de cuenta y comprobar aislamiento.

La cuenta ficticia usada para revisión se creó solo en localhost. Sus credenciales están fuera del repositorio y no se publican. No se cambió ninguna contraseña de cuentas existentes.

## Validación

Comandos y resultados definitivos se detallan en VALIDACION_BITACORA.md. No se puede compilar ni ejecutar iOS desde Windows; los actual de esa plataforma son mínimos, sin APIs nativas añadidas. La cámara física y las apps de terceros requieren revisión en un teléfono real.

En el backend Docker local actual, el módulo previo de Entreno puede responder que no tiene permiso/endpoints de rutinas. Mi Bitácora no depende de esas llamadas: los vínculos y sesiones aparecen cuando ya están en la caché existente.
