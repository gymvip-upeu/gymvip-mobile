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


## Integración de novedades de Acosta — 23/11/2026

Rama compartida: feature/e03-integracion-actualizaciones-qquerar. La referencia feature/e03-integracion-ma-rm-qquerar permanece intacta en los cuatro repositorios. Se integran los commits remotos revisados, sin modificar main ni las ramas personales de Acosta:

| Repositorio | Referencia E03 anterior | Acosta incorporado |
| --- | --- | --- |
| Backend | 653cba0 | 59e6ec5 |
| Comunicaciones | 29b0339 | 2005ad1 |
| Web | 71b5a31 | 4f5cf32 |
| Móvil | 94f5402 | 2e01fec |

### Contratos conciliados

- E03 conserva solo APP y WHATSAPP. Los registros históricos EMAIL no se borran; la recuperación de cuentas mantiene su infraestructura de correo independiente.
- Se conserva FAQ publicada, revisión de propuestas, desactivación, caché con error y contacto Recepción. APP se guarda antes del envío externo; lectura privada por socio y clave única por periodo/regla/fecha/canal. Reevaluar no reenvía fallidos.
- La API devuelve JSON por defecto aunque Firebase incorpore convertidores XML. Los formatos solicitados explícitamente, como las exportaciones, siguen disponibles. La anticipación acepta 3/5/7/15, en concordancia con la interfaz; la validación rechaza otros valores y elementos nulos.
- WhatsApp consulta al microservicio con clave interna y timeout. Si falla: UNVERIFIED. Producción incompleta se reporta UNCONFIGURED/BLOCKED y el adaptador rechaza el envío; se elimina el antiguo fallback silencioso a simulación. La simulación explícita continúa sin enviar mensajes incluso si hay credenciales. CONFIGURED_REAL no prueba conexión ni entrega.
- La bandeja funciona sin Firebase. Push integra Firebase Admin y el registro Android opcional; requiere cuenta de servicio privada del servidor, google-services.json del Android correspondiente y dispositivo autorizado. El worker verifica token activo, cliente activo y consentimiento antes del envío. Credenciales presentes no equivalen a entrega comprobada. No se activó Firebase real durante QA.
- Se conservan las novedades de catálogo, prioridades, perfil de personal/AUDITOR, sesiones, reportes, incidencias, archivo de instalaciones, pedidos y enlaces Culqi. Los favoritos devuelven su registro canónico de forma idempotente. Las promociones superpuestas se resuelven con prioridad y desempate determinista.
- Móvil mantiene SYSTEM carbón/azul/blanco y las paletas DARK/LIGHT previas. Conserva BLUE_DARK/BLUE_LIGHT y preferencias por cuenta. Debug permite Firebase deshabilitado; release exige Firebase oficial y firma privada, sin reemplazarla por la firma debug.
- Seguimiento sigue siendo el único historial operativo; resultados históricos independientes del estado actual de canales. Plantillas locales solo afectan la vista previa y su revisión/aprobación en ese navegador, no Meta ni el contenido efectivo del servidor. No se habilitan reintentos reales incompletos.

### Base y configuración de cada compañero

Se conserva la configuración local privada y el número de pruebas de Recepción en la compilación local, sin copiar el .env de Acosta ni publicar secretos. Cada entorno aporta sus propios .env y archivos privados. Backend y comunicaciones deben compartir el mismo token interno. Firebase del servidor usa FIREBASE_SERVICE_ACCOUNT_BASE64; no sustituye google-services.json ni las claves de firma Android. Culqi permanece sin cobros reales en este entorno.

Respaldo local antes de aplicar V21–V28: gymvip-before-actualizaciones.dump, fuera de Git. Base existente actualizada a V28 y base aislada de pruebas creada desde cero. V1–V20 se conservan; para bases de Acosta con checksums históricos V16/V17 diferentes sigue aplicando la conciliación explícita y verificada descrita arriba/en la documentación del backend. Nunca ejecutar repair genérico ni alterar la base de un compañero sin revisar y respaldar su historial. No se intervino ninguna base remota.

### Verificación de esta entrega

Backend: 260 pruebas; comunicaciones: 32; web: 100 y build; móvil: 53 pruebas de escritorio y APK Android debug instalado. Los reportes finales no presentan fallos. Pruebas de proveedor Meta/FCM/Culqi usan dobles o configuración local: no acreditan entregas ni cobros reales. No se compiló iOS ni se acredita una publicación Android en tienda.

Recorrido manual en entorno local, 23/11/2026:

1. FAQ QA2311: crear/publicar; guardar propuesta mantuvo la respuesta vigente en móvil; aprobar cambió la respuesta; desactivar la retiró tras actualizar.
2. Con backend detenido: el móvil mostró error, última respuesta guardada, Reintentar y contacto WhatsApp. Al recuperar backend se actualizó correctamente. La FAQ de prueba quedó desactivada.
3. Se ajustó exclusivamente la membresía de Sofia Demo para vencer el 26/11/2026 (periodo 28/10–27/11 exclusivo). El segundo socio de pruebas estaba vencido cinco días. Reglas con anticipación 3/5/7/15 y posteriores 1/5; solo APP/WHATSAPP.
4. Con comunicaciones detenido, Evaluar ahora registró cuatro avisos/intentos para dos periodos: APP disponible y WHATSAPP fallido. No se enviaron mensajes a números reales.
5. El móvil actualizó Avisos, abrió el recordatorio del 26/11 y confirmó lectura en backend. Seguimiento mostró Leído por el socio y su fecha. Al recuperar comunicaciones a SIMULATION, los intentos fallidos conservaron ese resultado.
6. Segunda evaluación: dos periodos, cero avisos nuevos y cuatro omitidos/repetidos. Se conserva trazabilidad, sin duplicados ni reenvío del fallo.

Evidencia local: capturas 63–68 y logs actualizaciones-* en outputs/ del espacio integrado. Backend 8081, comunicaciones 8085, web 5173 y emulador Android local. Programación diaria deshabilitada para pruebas; reglas habilitadas no significan cron habilitado. El emulador API36 usa compatibilidad 16KB para bibliotecas nativas heredadas. Próximo recorrido pendiente: reintento controlado WhatsApp con autorización, idempotencia y resultado real; no está implementado por esta integración.

Verificación adicional de canales: microservicio detenido → UNVERIFIED; bandeja AVAILABLE; recuperación → SIMULATION; historial completo idéntico antes/después; push UNCONFIGURED y connectionVerified=false. Configuración persistida exclusivamente WHATSAPP/APP. Evidencia outputs/69-integracion-canales.json. Contenedor de pruebas detenido al terminar; servicios de la aplicación permanecen encendidos.
