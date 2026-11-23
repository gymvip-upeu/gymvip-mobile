# Flujo push móvil E03 — avance guardado

Rama: `feature/e03-push-movil-qquerar`, derivada de la integración anterior. Trabajo pausado por indicación del usuario el 23/11/2026.

## Implementado

- Registro FCM vinculado a la sesión del socio, recuperación al reconectar y limpieza local antes del cierre remoto.
- Notificaciones genéricas sin datos privados en la pantalla de bloqueo; validación de cuenta/sesión y deduplicación por aviso.
- Apertura del aviso concreto desde el intent, con consulta de la bandeja propia; no confirma lectura automáticamente.
- Caché propia y reintento si el aviso aún no se descargó. Mensaje claro si dejó de estar disponible.
- Aviso en la interfaz cuando las alertas del teléfono están desactivadas, conservando el acceso a la bandeja.
- Compatibilidad con las actualizaciones de pedidos de Acosta, que abren los pedidos autenticados del catálogo.

## Estado de Firebase y pruebas

Proyecto real creado con autorización del usuario: `gym-vip-e03-pruebas-qquerar`, plan Spark. Registrado el paquete `com.gymvip.mobile.local`. No se descargó `google-services.json`, no se generó la credencial privada del servidor y no se activó FCM real. El navegador integrado no completó la descarga; el control de Chrome fue bloqueado por no poder verificar con certeza la URL.

La compilación local con Firebase deshabilitado pasó junto con 59 pruebas de escritorio y cuatro pruebas instrumentadas: permiso denegado con caché, duplicados, cuenta/sesión ajenas y bloqueo local al salir. La prueba instrumentada usa un token deliberadamente inválido y aislado, nunca credenciales reales ni una autenticación aceptada por el servidor.

Después de esa compilación se añadió la conservación de la ruta de pedidos y el aviso visual de permiso desactivado: esos últimos cambios quedan pendientes de compilación y repetición de pruebas al retomar. No se acredita entrega real con app abierta/cerrada ni la épica terminada.

Para retomar: descargar la configuración oficial del Android registrado, obtener la cuenta de servicio del mismo proyecto, configurar el servidor, compilar Android con Firebase real y ejecutar el recorrido completo, incluidos permisos, cierre de sesión, red y repeticiones. No añadir secretos al repositorio. La variante local utiliza `http://10.0.2.2:8081` y el contacto de pruebas acordado.
