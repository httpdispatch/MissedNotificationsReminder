package com.app.missednotificationsreminder.payment.billing.data.mappers

import com.android.billingclient.api.BillingClient
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuType

@BillingClient.SkuType
fun SkuType.toRemote(): String {
    return when (this) {
        SkuType.INAPP -> BillingClient.SkuType.INAPP
        SkuType.SUBS -> BillingClient.SkuType.SUBS
    }
}
