# Validación de Mi Bitácora — 26 de noviembre de 2026

## Compilación y pruebas

Entorno: Windows, JDK 21, Gradle 8.11.1, Kotlin 2.0.21, Compose Multiplatform 1.6.11. Android principal; Pixel 8 Pro API 37.1.

```powershell
.\gradlew.bat :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest '-Pgymvip.backendUrl=http://10.0.2.2:8081'
```

Resultado: BUILD SUCCESSFUL. desktopTest: 80 pruebas, 0 fallos, 0 errores. testDebugUnitTest: las mismas 80 pruebas comunes bajo el target Android, 0 fallos, 0 errores. No representan 160 escenarios distintos.

Las siete pruebas de JournalTest cubren restauración normalizada del rol CLIENTE/ADMIN, racha entre meses y con huecos, estadísticas combinadas sin contar dos veces el mismo día, Epley y mejor punto diario, validaciones, CRUD/aislamiento por cuenta, lectura inmutable de sesiones y privacidad de la tarjeta.

Dos pruebas instrumentadas pasaron en el emulador: generación PNG para las tres plantillas y dos tamaños, y comprobación opt-in de los roles de una sesión contra /auth/me real. Posteriormente se repitió la prueba de los seis PNG con el APK actualizado usando adb am instrument: OK (1 test). El instalador de connectedDebugAndroidTest retiró la app al finalizar; se reinstaló y se creó una cuenta CLIENTE ficticia nueva para la revisión manual. No se modificaron cuentas anteriores.

## Revisión manual observada

- Login con una cuenta CLIENTE ficticia del backend localhost, aceptación de consentimientos solo para esa cuenta y acceso a Entreno.
- Acceso a Mi Bitácora desde el botón de Entreno y el menú lateral.
- Registro del día con 45 minutos, músculos, ejercicio 3x10x20 kg y texto personal. Detalle: 600 kg de volumen.
- Persistencia del registro después de reinstalar con install -r y reabrir la app. Calendario con punto en el día y racha de un día.
- Edición de notas con modo avión activado: guardado y detalle correctos; conectividad restablecida al terminar.
- Evolución: punto Epley de 26 kg estimados, tiempo y volumen semanales, comparativa con el mes anterior.
- Vista previa PNG sin nombre/contactos ni notas privadas. Guardado confirmado por Snackbar y archivo en Pictures/GymVIP.
- ACTION_SEND abrió el selector nativo con miniatura. No se enviaron mensajes ni se publicó contenido.
- Photo Picker abrió la galería y aceptó una imagen del dispositivo.
- Denegar CAMERA produjo el mensaje de alternativa por galería; con permiso concedido se abrió la cámara del emulador, se tomó y confirmó una foto y volvió a la vista previa.
- Tema claro observado; el módulo utiliza tokens de tema para las superficies y textos. Las tarjetas exportadas usan fondos de marca oscuros propios.

## Límites de la comprobación

- iOS no puede compilarse en Windows. Se añadieron actual seguros, sin nuevos frameworks nativos, pero su compilación se debe comprobar en macOS.
- La captura/compartición se verificó en el emulador, no en un teléfono físico ni publicando en Instagram/WhatsApp.
- Android 7–9 ofrece compartir/guardar mediante otra app; MediaStore Pictures/GymVIP es desde Android 10.
- Las rutinas y sesiones disponibles dependen de la caché de Entreno. El Docker local actual responde un error de permisos a las llamadas nuevas de rutinas; no se cambió el backend en esta entrega. La bitácora local no necesita esos endpoints.
- La sincronización remota de la bitácora es una interfaz futura, sin llamadas ni endpoint implementado.

## Cambios de integración exactos

Ver INTEGRACION_BITACORA.patch: contiene el diff de las líneas de integración y las dos correcciones previas justificadas frente a origin/develop (5f95e1c). El catálogo está en su propio commit de esta rama. El esquema SQLDelight no tiene cambios.

