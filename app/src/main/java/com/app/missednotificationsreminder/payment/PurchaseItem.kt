package com.app.missednotificationsreminder.payment

import com.android.billingclient.api.SkuDetails

/**
 * The class to store purchase item information
 */
data class PurchaseItem(val skuDetails: SkuDetails)