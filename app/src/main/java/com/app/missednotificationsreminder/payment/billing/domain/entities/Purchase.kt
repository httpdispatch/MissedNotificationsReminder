package com.app.missednotificationsreminder.payment.billing.domain.entities

data class Purchase(
        val sku: String,
        val purchaseToken: String,
        val isAcknowledged: Boolean,
        val purchaseState: PurchaseState,
)
