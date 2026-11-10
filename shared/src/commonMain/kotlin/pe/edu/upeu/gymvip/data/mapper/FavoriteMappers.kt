package pe.edu.upeu.gymvip.data.mapper

import pe.edu.upeu.gymvip.data.local.Favorite_item
import pe.edu.upeu.gymvip.data.remote.dto.FavoriteResponseDto
import pe.edu.upeu.gymvip.domain.model.Favorite
import pe.edu.upeu.gymvip.domain.model.MoneyPen
import pe.edu.upeu.gymvip.domain.model.ProductCategory
import pe.edu.upeu.gymvip.domain.model.SyncStatus

fun FavoriteResponseDto.toDomain(fallbackAccountId: String): Favorite {
    val category = try {
        ProductCategory.valueOf(productCategory.uppercase())
    } catch (_: Exception) {
        ProductCategory.PLAN_MEMBRESIA
    }
    return Favorite(
        id = id,
        accountId = accountId ?: fallbackAccountId,
        productId = productId,
        productName = productName,
        productCategory = category,
        productPrice = MoneyPen.fromDouble(pricePen),
        personalNote = personalNote,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.SYNCED
    )
}

fun Favorite_item.toDomain(): Favorite {
    val category = try {
        ProductCategory.valueOf(productCategory.uppercase())
    } catch (_: Exception) {
        ProductCategory.PLAN_MEMBRESIA
    }
    val status = try {
        SyncStatus.valueOf(syncStatus.uppercase())
    } catch (_: Exception) {
        SyncStatus.SYNCED
    }
    return Favorite(
        id = id,
        accountId = accountId,
        productId = productId,
        productName = productName,
        productCategory = category,
        productPrice = MoneyPen(productPriceCents),
        personalNote = personalNote,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = status
    )
}
