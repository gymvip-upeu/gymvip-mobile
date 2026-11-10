# Tienda de suplementos (demo local)

El catálogo consulta `/api/v1/catalog/supplements` del backend real. Socio, administrador y recepción pueden abrir la ficha, ver imagen, nombre, descripción, presentación, contenido, ingredientes y precio; agregar unidades a un carrito local; y mostrar la lista en recepción. El carrito no crea pedidos, reserva stock ni cobra. Se vacía al cerrar la sesión. Solo ADMIN puede crear o modificar suplementos en el backend; RECEPCION y CLIENTE conservan acceso de lectura.

## Muestra inicial

Los nueve registros se cargaron en el backend local del 09-11-2026 mediante [`scripts/seed-catalogo-suplementos.ps1`](../scripts/seed-catalogo-suplementos.ps1). El script es idempotente por nombre y marca, pide una credencial ADMIN y no guarda contraseñas. Para repetirlo desde la raíz del proyecto: `./scripts/seed-catalogo-suplementos.ps1 -Credential (Get-Credential)`. Fotos originales generadas para esta demo en `assets/catalogo/`; **son ilustrativas, no fotos del envase real**. No se reutilizaron imágenes comerciales.

| Producto | Precio referencial | Fuente peruana |
|---|---:|---|
| Whey Protein Chocolate 500 g, Biocenter Natural | S/ 69.90 | [Inkafarma](https://inkafarma.pe/producto/Whey-Protein-Chocolate/1752683787611) |
| Creatine Monohydrate 500 g, Applied Nutrition | S/ 149.90 | [Inkafarma](https://inkafarma.pe/producto/creatine-monohydrate-500g-polvo-oral/076940) |
| Blood & Guts Fresa 300 g, DY Nutrition | S/ 129.00 | [Inkafarma](https://inkafarma.pe/producto/pre-entreno-blood-guts-fresa-polvo/072375) |
| Creatina Creapure Monohidratada 500 g, Dymatize | S/ 159.00 (antes S/ 209.00) | [NutriPoint Perú](https://www.nutripoint.com.pe/creatina-creapure-500g-dymatize/p), captura del usuario |
| Barra proteica Apple Pie 60 g, Quest | S/ 14.00 | [NutriPoint Perú](https://www.nutripoint.com.pe/barras-proteina-quest-60g/p), [Quest](https://www.questnutrition.com/products/apple-pie-protein-bars) |
| Pack dúo Gold Standard Protein Shake, Optimum Nutrition | S/ 20.00 (antes S/ 24.00) | [NutriPoint Perú](https://www.nutripoint.com.pe/snacks-saludables?order=OrderByScoreDESC&page=1) |
| Creatina micronizada 300 g, Optimum Nutrition | S/ 99.00 (antes S/ 139.00) | [NutriPoint Perú](https://www.nutripoint.com.pe/creatina-300g-optimum-nutrition/p), captura del usuario |
| ISO100 Hydrolyzed 5 lb, Dymatize | S/ 639.00 (antes S/ 659.00) | [NutriPoint Perú](https://www.nutripoint.com.pe/proteina/proteina-hidrolizada), captura del usuario |
| Galleta proteica 15 g proteína, Quest | S/ 14.00 | [NutriPoint Perú](https://www.nutripoint.com.pe/galleta-quest-protein-cookie/p) |

Los importes y la disponibilidad `DISPONIBLE` son datos de demostración. Antes de ofrecerlos como inventario real, administración debe confirmar productos, rotulado, precios, stock y condiciones comerciales con el gimnasio. La app indica confirmar precio y stock en recepción. Los cuatro precios tachados se cargaron como promociones de muestra por 30 días desde el 09-11-2026, con condiciones que obligan a confirmar con recepción. No son ofertas comprometidas por GYM VIP. Algunas capturas muestran precios distintos de los publicados actualmente por la tienda de referencia; se respetaron las cifras que envió el usuario como datos de muestra.

## Imágenes

Se usó la herramienta integrada de generación de imágenes. Las tres imágenes iniciales son `product-mockup` de potes genéricos, sin marcas ni texto, con fondo oscuro y acentos dorados: whey de chocolate 500 g (`whey-chocolate-ilustrativa.png`), creatina sin sabor 500 g (`creatina-ilustrativa.png`) y preentreno fresa 300 g (`preentreno-fresa-ilustrativa.png`). Los prompts pidieron fotografía de estudio, formato vertical 4:5 e indicaron que no se represente un envase comercial específico.

Las seis imágenes adicionales se generaron mediante edición con las capturas del usuario como referencia, pidiendo conservar solo el producto o pack en un fondo claro, quitar botones, precios, bordes y distintivos de la tienda, mantener colores y forma generales del envase y no inventar afirmaciones nutricionales. Sus archivos son `dymatize-creatina-500-ilustrativa.png`, `quest-barra-manzana-ilustrativa.png`, `on-shake-duo-ilustrativa.png`, `on-creatina-300-ilustrativa.png`, `dymatize-iso100-ilustrativa.png` y `quest-galleta-ilustrativa.png`. La app señala que las imágenes son ilustrativas.
