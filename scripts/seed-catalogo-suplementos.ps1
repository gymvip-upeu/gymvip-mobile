param(
    [Parameter(Mandatory = $true)][pscredential]$Credential,
    [string]$BaseUrl = 'http://localhost:8081'
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')
$login = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/login" -ContentType 'application/json' -Body (
    @{ email = $Credential.UserName; password = $Credential.GetNetworkCredential().Password } | ConvertTo-Json
)
if ('ADMIN' -notin $login.roles) { throw 'Solo una cuenta ADMIN puede cargar el catálogo.' }
$headers = @{ Authorization = "Bearer $($login.token)" }
$current = Invoke-RestMethod -Uri "$base/api/v1/catalog/supplements?size=60" -Headers $headers

# Precios de muestra consultados en Inkafarma, NutriPoint y capturas del usuario el 09-11-2026.
# No representan inventario ni una oferta comprometida por el gimnasio. Fotos ilustrativas.
$items = @(
    @{
        name = 'Whey Protein Chocolate 500 g'; brand = 'Biocenter Natural'; category = 'PROTEINA'
        presentation = 'Envase'; netContent = '500 g'; flavor = 'Chocolate'
        description = 'Mezcla en polvo para preparar una bebida con proteína de suero, soya, L-glutamina y vitaminas. Contiene leche y soya.'
        ingredients = 'Proteína de suero de leche, soya, L-glutamina, vitaminas C y D. Alérgenos: leche y soya.'
        pricePen = 69.90; image = 'whey-chocolate-ilustrativa.png'
        source = 'https://inkafarma.pe/producto/Whey-Protein-Chocolate/1752683787611'
    },
    @{
        name = 'Creatine Monohydrate 500 g'; brand = 'Applied Nutrition'; category = 'CREATINA'
        presentation = 'Pote'; netContent = '500 g'; flavor = 'Sin sabor'
        description = 'Creatina monohidratada en polvo para solución oral. Revisa las indicaciones y advertencias del envase antes de consumir.'
        ingredients = 'Creatina monohidratada.'
        pricePen = 149.90; image = 'creatina-ilustrativa.png'
        source = 'https://inkafarma.pe/producto/creatine-monohydrate-500g-polvo-oral/076940'
    },
    @{
        name = 'Blood & Guts Fresa 300 g'; brand = 'DY Nutrition'; category = 'PREENTRENO'
        presentation = 'Pote'; netContent = '300 g'; flavor = 'Fresa'
        description = 'Preentreno en polvo sabor fresa. Contiene cafeína; consulta el rotulado y sus precauciones antes de consumir.'
        ingredients = 'L-citrulina, beta alanina, L-arginina, L-tirosina, cafeína y nicotinamida, entre otros componentes indicados en el envase.'
        pricePen = 129.00; image = 'preentreno-fresa-ilustrativa.png'
        source = 'https://inkafarma.pe/producto/pre-entreno-blood-guts-fresa-polvo/072375'
    },
    @{
        name = 'Creatina Creapure Monohidratada 500 g'; brand = 'Dymatize'; category = 'CREATINA'
        presentation = 'Pote'; netContent = '500 g'; flavor = 'Sin sabor'
        description = 'Creatina monohidratada Creapure en polvo, sin sabor. Verifica el rotulado y las indicaciones del fabricante antes de usar.'
        ingredients = 'Creatina monohidratada Creapure.'
        pricePen = 209.00; offerPen = 159.00; image = 'dymatize-creatina-500-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/creatina-creapure-500g-dymatize/p'
    },
    @{
        name = 'Barra proteica Apple Pie 60 g'; brand = 'Quest'; category = 'OTROS'
        presentation = 'Barra individual'; netContent = '60 g'; flavor = 'Apple Pie'
        description = 'Barra proteica sabor pay de manzana. La presentación del fabricante indica 20 g de proteína por barra de 60 g.'
        ingredients = 'Mezcla de aislados de proteína de leche y suero, fibra, almendras y manzana. Contiene leche, almendras y soya.'
        pricePen = 14.00; image = 'quest-barra-manzana-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/barras-proteina-quest-60g/p'
    },
    @{
        name = 'Pack dúo Gold Standard Protein Shake'; brand = 'Optimum Nutrition'; category = 'PROTEINA'
        presentation = 'Pack de 2 cartones'; netContent = '2 × 325 ml'; flavor = 'Chocolate'
        description = 'Dos bebidas proteicas listas para tomar, sabor chocolate. Cada cartón indica 24 g de proteína.'
        ingredients = 'Proteínas de leche y suero; consulta el rotulado de cada cartón para la lista completa de ingredientes y alérgenos.'
        pricePen = 24.00; offerPen = 20.00; image = 'on-shake-duo-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/snacks-saludables?order=OrderByScoreDESC&page=1'
    },
    @{
        name = 'Creatina micronizada 300 g'; brand = 'Optimum Nutrition'; category = 'CREATINA'
        presentation = 'Pote'; netContent = '300 g'; flavor = 'Sin sabor'
        description = 'Creatina monohidratada micronizada en polvo. La presentación de 300 g contiene aproximadamente 60 porciones de 5 g.'
        ingredients = 'Creatina monohidratada.'
        pricePen = 139.00; offerPen = 99.00; image = 'on-creatina-300-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/creatina-300g-optimum-nutrition/p'
    },
    @{
        name = 'ISO100 Hydrolyzed 5 lb'; brand = 'Dymatize'; category = 'PROTEINA'
        presentation = 'Pote'; netContent = '5 lb (2.27 kg)'; flavor = 'Vainilla'
        description = 'Proteína de suero hidrolizada en polvo. La captura de referencia muestra un regalo; consulta en recepción si aplica al producto disponible.'
        ingredients = 'Aislado de proteína de suero hidrolizada. Consulta el rotulado para sabor, ingredientes completos y alérgenos.'
        pricePen = 659.00; offerPen = 639.00; image = 'dymatize-iso100-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/proteina/proteina-hidrolizada'
    },
    @{
        name = 'Galleta proteica 15 g proteína'; brand = 'Quest'; category = 'OTROS'
        presentation = 'Galleta individual'; netContent = '59 g'; flavor = 'Double Chocolate Chip'
        description = 'Galleta suave de doble chocolate. El fabricante indica 15 g de proteína por galleta.'
        ingredients = 'Proteínas de leche y suero; consulta el empaque para la lista completa de ingredientes y alérgenos.'
        pricePen = 14.00; image = 'quest-galleta-ilustrativa.png'
        source = 'https://www.nutripoint.com.pe/galleta-quest-protein-cookie/p'
    }
)

$promos = Invoke-RestMethod -Uri "$base/api/v1/catalog/promotions?state=TODAS&size=60" -Headers $headers
foreach ($item in $items) {
    $created = $current.content | Where-Object { $_.name -eq $item.name -and $_.brand -eq $item.brand } | Select-Object -First 1
    if ($created) {
        Write-Output "Ya existe: $($item.name)"
    } else {
        $data = @{
            name = $item.name; brand = $item.brand; category = $item.category
            presentation = $item.presentation; netContent = $item.netContent; flavor = $item.flavor
            description = $item.description; ingredients = $item.ingredients; pricePen = $item.pricePen
            availability = 'DISPONIBLE'; published = $true; reviewed = $true
            primaryImageKey = 'upload:0'
        } | ConvertTo-Json -Compress
        $image = Get-Item -LiteralPath (Join-Path $PSScriptRoot "../assets/catalogo/$($item.image)")
        $created = Invoke-RestMethod -Method Post -Uri "$base/api/v1/catalog/supplements" -Headers $headers -Form @{ data = $data; images = $image }
        Write-Output "Cargado: $($created.name) [$($created.id)]"
    }
    if ($item.offerPen -and -not ($promos.content | Where-Object { $_.supplement.id -eq $created.id -and $_.name -eq "Precio especial · $($item.name)" })) {
        $start = (Get-Date).Date
        $promotion = @{
            supplementId = $created.id; name = "Precio especial · $($item.name)"
            description = 'Precio de muestra basado en la captura proporcionada. Confirma el precio en recepción.'
            conditions = 'Válido en la demo durante 30 días desde la carga. Sujeto a confirmación de precio y stock por el gimnasio. Pago presencial.'
            offerPricePen = $item.offerPen; startDate = $start.ToString('yyyy-MM-dd')
            endDate = $start.AddDays(30).ToString('yyyy-MM-dd')
            enabled = $true; reviewed = $true; supplementVersion = $created.version
        } | ConvertTo-Json -Compress
        $saved = Invoke-RestMethod -Method Post -Uri "$base/api/v1/catalog/promotions" -Headers $headers -ContentType 'application/json' -Body $promotion
        Write-Output "Promoción de muestra: $($saved.supplement.name) S/ $($saved.offerPricePen)"
    }
}

Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/logout" -Headers $headers | Out-Null
