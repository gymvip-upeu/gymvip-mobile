# Fase 5 — pulido visual y QA final

9 de noviembre de 2026. La aplicación mantiene `MODO_DEMO=false` y el contrato REST real. El APK debug normal apunta al backend local en `http://10.0.2.2:8081`; el backend conserva ADMIN, RECEPCION y CLIENTE con sus permisos propios.

## Resultado visual

- Bienvenida y acceso ajustados a los prototipos oscuros y dorados. La imagen original generada para esta app está en `shared/src/commonMain/composeResources/drawable/bienvenida_gym.png`. Es una ilustración fotográfica del gimnasio, no una imagen del local real.
- Icono GV propio y splash nativo Android. Inicio presenta tarjetas de igual altura, estados de membresía, promociones con imagen autenticada y contador de Avisos. Catálogo, favoritos, Avisos, perfil y preferencias siguen el contraste, espaciado y jerarquía de los prototipos.
- El tema claro pinta también las barras del sistema. Se revisó con fuente Android al 150 %; luego se restauró la escala original. Los campos de acceso usan foco y acciones del teclado. Se mejoraron las etiquetas de iconos, favoritos y avisos para accesibilidad.
- La app excluye archivos, base y preferencias de la copia de seguridad de Android. No incluye credenciales en código ni en recursos.

## Pruebas

- Android: `:androidApp:assembleDebug :androidApp:lintDebug :shared:testAndroidHostTest --offline --console=plain` terminó BUILD SUCCESSFUL. **99 pruebas, 0 fallos, 0 omitidas** con las tres pruebas optativas reales habilitadas. El lint no reportó errores.
- Backend: `mvnw.cmd -o package` aprobó **144 pruebas, 0 fallos, 0 errores, 0 omitidas** con PostgreSQL aislado `gymvip_checks_jorge/gymvip_test_jorge`. Se corrigió con autorización el 500 de consentimiento y Avisos: dos consultas de solo lectura ya no solicitan `SELECT FOR UPDATE`. Las escrituras siguen bloqueando y las rutas mantienen sus roles.
- Las cuentas existentes de ADMIN y RECEPCION se probaron contra el backend local en 8081: inicio de sesión, restauración, lecturas permitidas, denegaciones y salida. Sus contraseñas y roles no se alteraron.
- Se inició un segundo proceso Java temporal del mismo backend, en 8082, exclusivamente para QA con base y esquema separados y socios ficticios. Allí se validaron activación inicial, consentimiento, cambio obligatorio de contraseña, Inicio, Membresía, pagos, catálogo, promociones, favoritos, Avisos, perfil, preferencias, FAQ, cierres y aislamiento entre socios. Se recorrieron estados vigente, por vencer, vencido y sin membresía. El proceso QA quedó detenido al terminar.
- En el emulador Pixel 8 Pro se revisaron visualmente las pantallas de socio, ADMIN y RECEPCION, el cambio de tema y fuente grande. Las capturas y contraseñas ficticias están en `qa/phase5/capturas/`, ignoradas por Git. El arranque en frío del APK final de 8081 y el panel de RECEPCION también se comprobaron.

## Instalación local consolidada

Hay **un** contenedor backend GYM VIP activo: `gymvip-backend-s3-mobiles-v2`, imagen `gymvip-backend:fase5`, puerto 8081. PostgreSQL es otro contenedor necesario para los datos, `gymvip-sprint1-jorge-db`, con el mismo volumen `gymvip_sprint1_jorge_data`. Windows bloqueó los puertos locales 55432 y 55433 tras reiniciar Docker, por lo que PostgreSQL se publicó en 127.0.0.1:15432 y la URL JDBC del backend se actualizó. Las copias antiguas de backend se eliminaron: tenían política `unless-stopped`, que explicaba por qué reaparecieron y consumían CPU tras reiniciar Docker. `/api/v1/health` responde `UP`.

Para un servidor de QA aislado, `../gymvip-backend/qa/phase5/StartQa.ps1` valida la URL y el esquema de pruebas antes de arrancar; no debe mantenerse activo para usar la app normal. El APK de QA puede compilarse solo en debug con `-Pgymvip.debugApi=http://10.0.2.2:8082`; el APK normal omite ese parámetro y usa 8081. Las credenciales QA se guardan únicamente en metadatos locales ignorados por Git. `qa/phase5/AndroidUi.ps1` automatiza el recorrido del emulador sin imprimir contraseñas.

## Límites de esta entrega

No se compiló iOS porque este host Windows no tiene Xcode. La firma release y distribución requieren las claves privadas y el destino HTTPS real. El número oficial de WhatsApp no está configurado; Ayuda remite a recepción. El texto legal sigue provisional. La app usa backend real; el modo demo intercambiable del prompt maestro no se implementó. El entorno de QA usa datos ficticios y no sustituye una prueba con una cuenta de socio del gimnasio.
