package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails

/**
 * The class to store purchase item information
 */
data class PurchaseItem(val skuDetails: SkuDetails)
