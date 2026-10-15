package com.gymvip.mobile.data.mapper

import com.gymvip.mobile.data.local.Favorite_item
import com.gymvip.mobile.data.remote.dto.FavoriteResponseDto
import com.gymvip.mobile.domain.model.Favorite
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.domain.model.SyncStatus

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
