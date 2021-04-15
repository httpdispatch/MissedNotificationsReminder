package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails

/**
 * The class to store purchase item view state information
 */
data class PurchaseItemViewState(val skuDetails: SkuDetails) {
    val price: String = skuDetails.price
}
