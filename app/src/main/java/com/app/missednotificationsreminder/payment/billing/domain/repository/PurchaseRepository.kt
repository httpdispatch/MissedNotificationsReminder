package com.app.missednotificationsreminder.payment.billing.domain.repository

import android.app.Activity
import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper
import com.app.missednotificationsreminder.payment.billing.domain.entities.ConsumeResult
import com.app.missednotificationsreminder.payment.billing.domain.entities.Purchase
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuType

interface PurchaseRepository {
    /**
     * Get the sku details for the specified [skuList] of the [skuType] product type
     */
    suspend fun getSkuDetails(
            skuList: List<String>,
            skuType: SkuType
    ): ResultWrapper<List<SkuDetails>>

    /**
     * Launch the purchase flow for the specified product details
     */
    suspend fun purchase(
            skuDetails: SkuDetails,
            oldSku: String? = null,
            oldPurchaseToken: String? = null,
            userId: String? = null,
            activity: Activity
    ): ResultWrapper<List<Purchase>>

    /**
     * Query purchases for the specified [skuType]
     */
    suspend fun queryPurchases(skuType: SkuType): ResultWrapper<List<Purchase>>

    /**
     * Acknowledge [purchase] to avoid purchase transaction rollback
     */
    suspend fun acknowledgePurchase(purchase: Purchase): ResultWrapper<Boolean>

    /**
     * Consume the [purchase] so user may but it again
     */
    suspend fun consumePurchase(purchase: Purchase): ResultWrapper<String>

    /**
     * Check whether there are any unhandled purchases and try to handle them
     */
    suspend fun verifyAndConsumePendingPurchases(): ConsumeResult
}
