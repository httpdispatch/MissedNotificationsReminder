package com.app.missednotificationsreminder.payment.billing.domain.entities

data class SkuDetails(
        val sku: String,
        val price: String,
        val priceAmountMicros: Long,
        val skuType: SkuType,
)
