# Integración E03 MA/RM

La rama integra Acosta con el recorrido E03 de Qquerar. Se conservan los repositorios de FAQ/Avisos, confirmación remota de lectura, actualización y manejo de errores. El mapper acepta POR_VENCER/PROXIMA_A_VENCER y VENCIDO/VENCIDA; la caché de perfil usa el identificador de cuenta autenticada, distinto del identificador de cliente.

## Desarrollo local compartido

Requisitos: JDK21, Android SDK34, backend en 8081 y emulador Android. No se requieren servicios Firebase para este modo.

~~~powershell
./gradlew.bat :composeApp:assembleDebug :composeApp:desktopTest '-Pgymvip.firebase=false' '-Pgymvip.backendUrl=http://10.0.2.2:8081' '-Pgymvip.debugApplicationId=com.gymvip.mobile.local'
~~~

El contacto oficial de recepción es el valor predeterminado. Para pruebas de contacto se puede añadir -Pgymvip.receptionWhatsApp=NUMERO_E164 (sin espacios). No se cambia el contacto de producción. HTTP se permite exclusivamente en el manifiesto debug; release conserva HTTPS oficial.

## Servidor remoto y Firebase del equipo

El debug predeterminado apunta a Render y usa com.gymvip.mobile, como la rama de Acosta. Se puede compilar sin Firebase usando -Pgymvip.firebase=false. Para Firebase real, el propietario debe obtener google-services.json del proyecto compartido, registrar el applicationId correcto y colocar el archivo en composeApp/google-services.json o composeApp/src/debug/google-services.json. Un archivo registrado para com.gymvip.mobile no valida automáticamente com.gymvip.mobile.local; registrar ese paquete por separado para local con Firebase.

~~~powershell
./gradlew.bat :composeApp:assembleDebug '-Pgymvip.firebase=true'
~~~

Sin archivo el modo real falla con una explicación; no se generan credenciales falsas. El archivo no apareció en las ramas ni en su historial durante esta integración y no viene dentro del .env de backend. google-services.json, keystores y contraseñas siguen excluidos de Git. Release exige configuración Firebase real y firma privada distinta de debug.

El servicio FCM y sus adaptadores se conservan. La prueba de una notificación real necesita además la configuración y emisor FCM del proyecto; recibir en la bandeja no depende de Firebase ni de un envío WhatsApp. No se verificó entrega FCM real en esta entrega.

## Apariencia y navegación

SYSTEM conserva negro carbón/azul/texto blanco; DARK y LIGHT conservan las paletas acordadas. Los estilos azules de Acosta quedan disponibles con claves independientes BLUE_DARK/BLUE_LIGHT, y su alternativa DARK_GOLD se conserva. Accesibilidad, escalado y reducción de movimiento continúan disponibles. Navegación y Cuenta y seguridad están conciliadas sin destinos duplicados.

## Verificación

51 pruebas de escritorio y APK Android local correctos. FAQ: crear/publicar/editar, conservar versión vigente mientras hay propuesta, publicar actualización, desactivar y actualizar móvil. Con backend detenido se informa el fallo con caché y contacto Recepción; Reintentar recupera. Avisos: evaluar con proveedor detenido, APP disponible, externos FALLIDO, marcar leído, historial web LEIDO y reevaluación con cero nuevos. Las pruebas sobre API/base compartida deben hacerse con registros de prueba y sin cambiar credenciales existentes.

El emulador API36 puede mostrar modo de compatibilidad 16KB por librerías nativas CameraX/MLKit heredadas; se ejecutó en ese modo. La adecuación de esas librerías para publicación en tienda queda fuera de esta integración E03.
