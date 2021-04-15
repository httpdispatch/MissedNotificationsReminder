package com.app.missednotificationsreminder.payment.billing.domain.entities

object BillingErrorCodes {
    const val BILLING_UNAVAILABLE = 3
    const val SERVICE_UNAVAILABLE = 2
    const val ITEM_ALREADY_OWNED = 7
    const val USER_CANCELED = 1

    /**
     * The purchase payment is pending
     */
    const val PURCHASE_PENDING = 10
}
