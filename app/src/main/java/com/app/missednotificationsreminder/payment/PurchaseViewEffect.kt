package com.app.missednotificationsreminder.payment

import com.android.billingclient.api.SkuDetails

sealed class PurchaseViewEffect {
    /**
     * Purchase the specified [skuDetails]
     */
    data class Purchase(val skuDetails: SkuDetails) : PurchaseViewEffect()

    /**
     * Show the specified [message]
     */
    data class Message(val message: String) : PurchaseViewEffect()
}