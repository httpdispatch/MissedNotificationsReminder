package com.app.missednotificationsreminder.payment

import com.android.billingclient.api.SkuDetails

/**
 * The class to store purchase item view state information
 */
data class PurchaseItemViewState(val skuDetails: SkuDetails) {
    val price: String = skuDetails.price
}