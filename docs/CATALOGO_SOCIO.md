# Catálogo del socio: tarjeta y detalle

Actualización local del 25/11/2026 en feature/e03-acceso-recuperacion-correo-qquerar.

La tarjeta mantiene la imagen completa centrada (ContentScale.Fit), disponibilidad sobre la foto, categoría, nombre, marca y presentación en el orden sabor · contenido · presentación. El precio tiene jerarquía visual; una oferta vigente muestra precio anterior tachado, precio vigente y descuento sin reutilizar promociones históricas. Un descuento inferior al redondeo de 1% se muestra como Oferta, sin inventar un porcentaje.

Se usan exclusivamente los colores del tema Material 3 actual y un borde estático con acento de baja opacidad. El precio y las acciones de texto usan el acento solo si alcanza contraste suficiente contra la superficie; en otro caso toman onSurface. No se modificaron las definiciones de los temas.

La grilla usa un máximo de dos columnas, con tarjeta mínima de 180 dp escalada por el tamaño de texto del sistema y de la app. En pantallas estrechas o con texto aumentado usa una columna. Mantiene filtros, búsqueda, favoritos, categorías y carrito existentes.

Al tocar la tarjeta o Ver detalle se abre un panel desplazable. Incluye imagen completa, presentación, descripción, ingredientes, precio y oferta con vigencia/condiciones cuando existen. Las acciones Agregar al carrito y Consultar a recepción quedan al pie. El detalle puede cerrarse y no ofrece edición administrativa.

Agregar sigue usando el mismo callback y API del carrito. Solo se habilita para rol de socio con acción de compra disponible, producto SUPLEMENTO disponible y vista que no sea de promociones finalizadas. No se consulta ni exige membresía activa. Un agotado conserva su detalle y muestra la compra deshabilitada. En Finalizadas se muestra el precio regular y el precio histórico se etiqueta explícitamente como no vigente.

Verificación: 72 pruebas móviles y compilación APK correctas. Se verificaron detalle, presentación y adaptación a texto 130% en el emulador. Hay pruebas de regresión para disponibilidad, roles de compra, promociones históricas, precio y tamaño de columnas. No se crearon productos ni se modificaron datos de catálogo, backend o web. La base local contiene una imagen de demostración de color plano: para evaluar el empaque real se necesita la fotografía registrada del producto.

Incidencia independiente detectada al probar: GET /api/v1/member-app/cart responde 500 al crear el carrito del socio actual; SupplementOrderService envía java.time.Instant como parámetro JDBC y PostgreSQL no infiere el tipo. Incidencia posteriormente corregida y validada en backend; consultar gymvip-backend/docs/CARRITO_JDBC_UTC.md. La prueba del catálogo no realizó cobros ni confirmó pedidos reales.

Sin commit ni publicación automática de este cambio. Se conserva la corrección de roles de Membresía que ya estaba pendiente en el mismo repositorio.

Verificación final 25/11/2026: 280 pruebas backend y 72 móviles correctas, empaquetados correctos. Backend y app del emulador actualizados. En la cuenta de prueba se consultó carrito vacío sin 500, se agregó un suplemento desde el detalle, se abrió el carrito con una unidad y se retiró esa unidad, regresando al catálogo con cero productos y sin mensaje de error. No se confirmó ningún pedido ni se realizó pago. Cambios locales sin publicar.

## Publicación web y actualización móvil — 25/11/2026

El botón general Sincronizar datos llama también a CatalogViewModel.refresh. La vista muestra progreso y evita consultas simultáneas. Una respuesta correcta reemplaza el listado completo, deduplicado por ID: un suplemento ocultado deja de aparecer incluso si antes estaba en memoria. No se exige membresía activa para consultar ni agregar; la autorización depende del rol CLIENTE.

Ante un fallo se conserva la última respuesta completa de la sesión, con aviso explícito de Información anterior y Reintentar. Mientras se actualiza o los datos son anteriores no se ofrece agregar al carrito desde esas tarjetas; el detalle también explica la situación. Una primera consulta fallida no inventa productos ni afirma tener datos anteriores. La caché SQL existente conserva sus campos anteriores; no se añadió almacenamiento persistente de todos los metadatos. La conservación completa probada corresponde a la sesión abierta.

Publicación, disponibilidad y stock son distintos. Publicar no crea unidades; el backend rechaza altas al carrito de agotados u ocultos y sigue verificando existencias/reservando únicamente al confirmar pedido.

Pruebas: 76 móviles; 283 backend en suite completa y 15 de catálogo repetidas con un socio registrado sin ninguna membresía; 102 web. En emulador y web local: S/89.90 → S/90.50, una sola tarjeta, detalle agotado con acción deshabilitada, ocultación tras dos actualizaciones, indisponibilidad del servidor, aviso de datos anteriores y recuperación con Reintentar. Se usó temporalmente el suplemento existente; al terminar se restauraron S/89.90, Disponible y publicado, y se conservó el historial. No se compró ni se emitieron pagos.

Pendiente para cerrar la demostración solicitada con datos realistas: registrar un producto nuevo con una fotografía limpia del empaque. No se publicaron fichas con imágenes falsas. La descarga de la fotografía del fabricante fue rechazada por la revisión automática. Los registros de la prueba automatizada usan imagen técnica y transacción revertida en PostgreSQL de pruebas, no se añadieron al catálogo local del usuario.
