package com.app.missednotificationsreminder.payment.billing.data.mappers

import com.android.billingclient.api.Purchase
import com.app.missednotificationsreminder.payment.billing.domain.entities.PurchaseState

fun Purchase.toDomain() = com.app.missednotificationsreminder.payment.billing.domain.entities.Purchase(
        sku = sku,
        purchaseToken = purchaseToken,
        isAcknowledged = isAcknowledged,
        purchaseState = purchaseState.toPurchaseStateDomain(),
)

private fun Int.toPurchaseStateDomain(): PurchaseState {
    return when (this) {
        Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
        Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
        else -> PurchaseState.UNSPECIFIED_STATE
    }
}
