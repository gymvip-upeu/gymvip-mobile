# Ajuste autorizado — lectura del catálogo por socios

Autorizado expresamente por el usuario y aplicado el 9 nov 2026 en `gymvip-backend`, rama `feature/post-revision-diseno-qa`. Este archivo conserva la propuesta revisada y ahora registra su aplicación. Se permite únicamente la lectura GET descrita abajo. La suite completa del backend aprobó 142 pruebas, incluida lectura de productos/imágenes publicados para CLIENTE, denegación de ocultos y permisos de personal.

`CatalogController` declara CLIENTE entre sus roles permitidos y filtra productos publicados y promociones vigentes para ese rol. La regla HTTP global de `SecurityConfig`, en cambio, deja todas las rutas de catálogo bajo ADMIN/RECEPCION. El móvil no puede resolver ese permiso cambiando su interfaz.

Ajuste mínimo autorizado y aplicado antes de la regla final `anyRequest`, manteniendo las anotaciones y comprobaciones de publicación existentes:

```java
.requestMatchers(HttpMethod.GET,
    "/api/v1/catalog/options",
    "/api/v1/catalog/supplements",
    "/api/v1/catalog/supplements/*",
    "/api/v1/catalog/supplements/*/images/*",
    "/api/v1/catalog/promotions",
    "/api/v1/catalog/promotions/*")
.hasAnyRole("ADMIN", "RECEPCION", "CLIENTE")
```

Los patrones permiten segmentos concretos de consulta; no abren POST/PUT/PATCH, historiales administrativos ni otras rutas. Las escrituras siguen exigiendo ADMIN en el controlador. Este cambio no habilita promociones finalizadas para CLIENTE: ese límite es del servicio y necesita una decisión separada si se desea cambiarlo.

Validación ejecutada: cadena de seguridad y anotaciones reales para ADMIN/RECEPCION/CLIENTE/anónimo, PostgreSQL aislado para lectura CLIENTE y 404 de productos/imágenes ocultos. Escrituras e historiales siguen restringidos a ADMIN. También se corrigió un error previo del filtro `published` nulo que impedía consultar a personal sin ese filtro. La prueba positiva CLIENTE usa el contexto de seguridad de Spring en la integración con PostgreSQL; no se presenta como un login con credenciales de un socio real del gimnasio.
