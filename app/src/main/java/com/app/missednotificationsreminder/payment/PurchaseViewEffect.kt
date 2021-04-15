package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails

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
