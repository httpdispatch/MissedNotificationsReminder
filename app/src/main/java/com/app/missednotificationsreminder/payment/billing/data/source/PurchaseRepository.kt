package com.app.missednotificationsreminder.payment.billing.data.source

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.ResultWrapper
import com.app.missednotificationsreminder.data.collectWithLastErrorOrSuccessStatus
import com.app.missednotificationsreminder.data.flatMap
import com.app.missednotificationsreminder.data.onErrorReturn
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.data.source.BillingErrorCodes.handleBillingError
import com.app.missednotificationsreminder.payment.billing.data.source.remote.BillingOperationException
import com.app.missednotificationsreminder.payment.billing.data.source.remote.CoroutinesBilling
import com.app.missednotificationsreminder.util.coroutines.retryCallOnError
import com.app.missednotificationsreminder.util.loadingstate.BasicLoadingStateManager
import com.app.missednotificationsreminder.util.loadingstate.HasLoadingStateManager
import com.app.missednotificationsreminder.util.loadingstate.LoadingStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext

class PurchaseRepository(scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
                         private val resourceDataSource: ResourceDataSource,
                         context: Context) :
        HasLoadingStateManager {
    val billing by lazy { CoroutinesBilling(scope, context) }
    override val loadingStateManager: LoadingStateManager by lazy { BasicLoadingStateManager() }

    private suspend fun <T> processBillingCall(
            operationName: String,
            maxRetryCount: Int,
            block: suspend () -> T): ResultWrapper<T> {
        return attachLoading(operationName) {
            retryCallOnError(maxRetryCount) {
                block()
            }.onErrorReturn {
                it.handleBillingError(resourceDataSource)
            }
        }
    }

    /**
     * Get the sku details for the specified [skuList] of the [skuType] product type
     */
    suspend fun getSkuDetails(skuList: List<String>, @BillingClient.SkuType skuType: String): ResultWrapper<List<SkuDetails>> {
        Timber.d("getSkuDetails() called with: skuList = %s; skuType = %s",
                skuList, skuType)

        return attachLoading("getSkuListDetails") {
            attachLoadingStatus("Loading tariffs information") {
                skuList.asFlow()
                        .buffer()
                        .map { processBillingCall("getSkuDetails", 2) { billing.getSkuDetails(listOf(it), skuType) } }
                        .collectWithLastErrorOrSuccessStatus(ResultWrapper.Success(emptyList())) { acc, value ->
                            acc + value
                        }
            }
        }
    }

    /**
     * Launch the purchase flow for the specified product details
     */
    suspend fun purchase(skuDetails: SkuDetails, oldSku: String? = null, oldPurchaseToken: String? = null, userId: String? = null, activity: Activity): ResultWrapper<List<Purchase>> {
        Timber.d("purchase() called with: skuDetails = %s; oldSku = %s; oldPurchaseToken = %s; userId = %s; activity = %s",
                skuDetails, oldSku, oldPurchaseToken, userId, activity)
        return processBillingCall("purchase", 0) {
            try {
                billing.purchase(skuDetails, oldSku, oldPurchaseToken,
                        userId, activity)
            } catch (t: Throwable) {
                if (oldSku != null &&
                        t is BillingOperationException &&
                        t.code == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                    // Workaround for DEVELOPER_ERROR which may happen when old purchase
                    // has PURCHASED state but has been refunded or user is purchasing already purchased item
                    billing.purchase(skuDetails,
                            null, null,
                            userId, activity)
                } else {
                    throw t
                }
            }
        }
    }

    /**
     * Query purchases for the specified [skuType]
     */
    suspend fun queryPurchases(@SkuType skuType: String): ResultWrapper<List<Purchase>> {
        Timber.d("queryPurchases() called with: skuType = %s",
                skuType)
        return processBillingCall("purchase", 2) {
            billing.queryPurchases(skuType)
        }
    }

    /**
     * Acknowledge [purchase] to avoid purchase transaction rollback
     */
    suspend fun acknowledgePurchase(purchase: Purchase): ResultWrapper<Boolean> {
        Timber.d("acknowledgePurchase() called with: purchase = %s",
                purchase)
        return processBillingCall("acknowledgePurchase", 2) {
            billing.acknowledgePurchase(purchase.purchaseToken)
            true
        }
    }

    /**
     * Consume the [purchase] so user may but it again
     */
    suspend fun consumePurchase(purchase: Purchase): ResultWrapper<String> {
        Timber.d("consumePurchase() called with: purchase = %s",
                purchase)
        return processBillingCall("consumePurchase", 2) {
            billing.consumePurchase(purchase.purchaseToken)
        }
    }

    /**
     * Check whether there are any unhandled purchases and try to handle them
     */
    suspend fun verifyAndConsumePendingPurchases(): ResultWrapper<List<SkuDetails>> {
        Timber.d("verifyAndConsumePendingPurchases() called")
        return attachLoading("verifyAndConsumePendingPurchases") {
            attachLoadingStatus("Verifying and consuming pending purchases") {
                flowOf(SkuType.INAPP, SkuType.SUBS)
                        .map { skuType ->
                            queryPurchases(skuType)
                                    .flatMap {
                                        acknowledgePurchases(it, skuType == SkuType.INAPP, skuType)
                                    }
                        }
                        .collectWithLastErrorOrSuccessStatus(ResultWrapper.Success(emptyList())) { acc, value -> acc + value }
            }
        }
    }

    private suspend fun acknowledgePurchases(purchases: List<Purchase>, consumable: Boolean, skuType: String): ResultWrapper<List<SkuDetails>> {
        return purchases.asFlow()
                .filter { !it.isAcknowledged || consumable }
                .flatMapMerge(concurrency = 1) { purchase ->
                    flow {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                acknowledgePurchase(purchase)
                                        .flatMap {
                                            consumePurchase(purchase)
                                                    .flatMap { getSkuDetails(listOf(purchase.sku), skuType) }
                                        }
                                        .also { emit(it) }
                            }
                            Purchase.PurchaseState.PENDING ->
                                emit(ResultWrapper.Error(code = BillingErrorCodes.PURCHASE_PENDING,
                                        message = resourceDataSource.getString(R.string.payment_error_purchase_pending)))
                            else -> {
                            }// do nothing
                        }
                    }
                }
                .collectWithLastErrorOrSuccessStatus(ResultWrapper.Success(emptyList<SkuDetails>())) { acc, value ->
                    acc + value
                }
    }
}