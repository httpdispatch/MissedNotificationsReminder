package com.app.missednotificationsreminder.payment.billing.data.mappers

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuType

fun SkuDetails.toDomain() = com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails(
        sku = sku,
        skuType = type.toDomainSkuType(),
        price = price,
        priceAmountMicros = priceAmountMicros,
)

private fun String.toDomainSkuType(): SkuType {
    return when (this) {
        BillingClient.SkuType.INAPP -> SkuType.INAPP
        BillingClient.SkuType.SUBS -> SkuType.SUBS
        else -> throw IllegalArgumentException("Unsupported sku type $this")
    }
}
