package com.app.missednotificationsreminder.payment.billing.data.source.remote

import android.app.Activity
import android.content.Context
import androidx.annotation.UiThread
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import com.app.missednotificationsreminder.util.coroutines.CoroutinesQueue
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The coroutines wrapper around google's [BillingClient]
 *
 * Inspired by https://github.com/mu29/rx-billing
 */
class CoroutinesBilling(
        scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
        context: Context) {
    private val billingClient by lazy {
        BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(purchaseListener)
                .build()
    }
    private val purchaseListener by lazy { PurchaseListener() }
    private val queue by lazy { CoroutinesQueue(scope) }
    private val scopeCompletionListener: DisposableHandle?

    init {
        scopeCompletionListener = scope.coroutineContext[Job]?.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                Timber.d("Billing completion")
                shutdown()
            }
        }
    }

    /**
     * Get the sku details for the specified `sku`s
     *
     * @param skuList
     * @param skuType the product type
     * @return
     */
    suspend fun getSkuDetails(skuList: List<String>, @SkuType skuType: String): List<SkuDetails> {
        Timber.d("getSkuDetails() called with: skuList = %s; skuType = %s",
                skuList, skuType)
        return queue.launchInQueue {
            tryConnect()
            suspendCancellableCoroutine { continuation ->
                val params = SkuDetailsParams.newBuilder()
                        .setSkusList(skuList)
                        .setType(skuType)
                        .build()
                billingClient.querySkuDetailsAsync(params) { billingResult: BillingResult, skuDetailsList: List<SkuDetails?>? ->
                    Timber.d("onSkuDetailsResponse() called with: billingResult = %s; skuDetailsList = %s",
                            asString(billingResult), skuDetailsList)
                    if (continuation.isCompleted) {
                        return@querySkuDetailsAsync
                    }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(skuDetailsList
                                ?.filterNotNull()
                                ?: emptyList())
                    } else {
                        continuation.resumeWithException(SkuDetailsFailureException(billingResult))
                    }
                }
            }
        }
    }

    /**
     * Query purchases of the specified `skuType`
     *
     * @param skuType
     * @return
     */
    suspend fun queryPurchases(@SkuType skuType: String): List<Purchase> {
        Timber.d("queryPurchases() called with: skuType = %s",
                skuType)
        return queue.launchInQueue {
            tryConnect()
            suspendCancellableCoroutine { continuation ->
                val result = billingClient.queryPurchases(skuType)
                Timber.d("queryPurchasesResponse() called with: billingResult = %s, purchasesList = %s",
                        asString(result.billingResult), result.purchasesList)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(result.purchasesList
                            ?.filterNotNull()
                            ?: emptyList())
                } else {
                    continuation.resumeWithException(QueryPurchaseFailureException(result.billingResult))
                }
            }
        }
    }

    /**
     * Query purchases history of the specified `skuType`
     *
     * @param skuType
     * @return
     */
    suspend fun queryPurchasesHistory(@SkuType skuType: String): List<PurchaseHistoryRecord> {
        Timber.d("queryPurchases() called with: skuType = %s",
                skuType)
        return queue.launchInQueue {
            tryConnect()
            suspendCancellableCoroutine { continuation ->
                billingClient.queryPurchaseHistoryAsync(skuType) { billingResult: BillingResult,
                                                                   purchaseHistoryRecordList: List<PurchaseHistoryRecord?>? ->
                    Timber.d("onPurchaseHistoryResponse() called with: billingResult = %s, purchaseHistoryRecordList = %s",
                            asString(billingResult), purchaseHistoryRecordList)
                    if (continuation.isCompleted) {
                        return@queryPurchaseHistoryAsync
                    }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(
                                purchaseHistoryRecordList
                                        ?.filterNotNull()
                                        ?: emptyList())
                    } else {
                        continuation.resumeWithException(QueryPurchaseFailureException(billingResult))
                    }
                }
            }
        }
    }

    /**
     * Acknowledge the purchase with the specified purchase token
     *
     * @param purchaseToken
     * @return
     */
    suspend fun acknowledgePurchase(purchaseToken: String) {
        Timber.d("acknowledgePurchase() called with: purchaseToken = %s",
                purchaseToken)
        return queue.launchInQueue {
            runCatching {
                tryConnect()
                suspendCancellableCoroutine<Unit> { continuation ->
                    val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(params) { billingResult: BillingResult ->
                        Timber.d("onAcknowledgePurchaseResponse() called with: billingResult = %s",
                                asString(billingResult))
                        if (!continuation.isCompleted) {
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                continuation.resume(Unit)
                            } else {
                                continuation.resumeWithException(AcknowledgePurchaseFailureException(billingResult))
                            }
                        }
                    }
                }
            }.onFailure { t ->
                if (t is BillingOperationException &&
                        t.code == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                    // Workaround for DEVELOPER_ERROR which may happen when purchase
                    // has PURCHASED state but has been refunded
                    return@onFailure
                } else {
                    throw t
                }
            }
        }
    }

    /**
     * Consume the purchase with the specified token
     *
     * @param purchaseToken
     * @return
     */
    suspend fun consumePurchase(purchaseToken: String): String {
        Timber.d("consumePurchase() called with: purchaseToken = %s",
                purchaseToken)
        return queue.launchInQueue {
            tryConnect()
            suspendCancellableCoroutine { continuation ->
                val params = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchaseToken).build()
                billingClient.consumeAsync(params) { billingResult: BillingResult, token: String? ->
                    Timber.d("onConsumeResponse() called with: billingResult = %s; token = %s",
                            asString(billingResult), token)
                    if (continuation.isCompleted) {
                        return@consumeAsync
                    }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(token ?: "")
                    } else {
                        continuation.resumeWithException(ConsumePurchaseFailureException(billingResult))
                    }
                }
            }
        }
    }

    /**
     * Launch billing purchase flow
     *
     * @param skuDetails the product details to buy
     * @param oldSku     the optional previous sku to replace with the new product (used for subscriptions)
     * @param oldPurchaseToken the optional old purchase token to replace with the new product (used for subscriptions)
     * @param accountId  the user account id in drill.city
     * @param activity   the activity instance
     * @return
     */
    suspend fun purchase(
            skuDetails: SkuDetails,
            oldSku: String?,
            oldPurchaseToken: String?,
            accountId: String?,
            activity: Activity): List<Purchase> {
        Timber.d("purchase() called with: " +
                "skuDetails = $skuDetails, oldSku = $oldSku, oldPurchaseToken = $oldPurchaseToken, " +
                "accountId = $accountId, activity = $activity")
        return queue.launchInQueue {
            tryConnect()
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    purchaseListener.continuation = continuation
                    val params = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .setReplaceSkusProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                            .apply {
                                if (oldSku != null && oldPurchaseToken != null) {
                                    setOldSku(oldSku, oldPurchaseToken)
                                }
                                if (accountId != null) {
                                    setObfuscatedAccountId(accountId)
                                }
                            }
                            .build()
                    billingClient.launchBillingFlow(activity, params)
                }
            }
        }
    }

    /**
     * Try connect to the billing
     *
     * @return
     */
    private suspend fun tryConnect() {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                if (billingClient.isReady) {
                    continuation.resume(Unit)
                    return@suspendCancellableCoroutine
                }
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        Timber.d("onBillingSetupFinished() called with: billingResult = %s",
                                asString(billingResult))
                        if (continuation.isCompleted) {
                            return
                        }
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(ConnectionFailureException(billingResult))
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.d("onBillingServiceDisconnected() called")
                    }
                })
            }
        }
    }

    @UiThread
    fun shutdown() {
        Timber.d("shutdown() called")
        purchaseListener.continuation = null
        billingClient.endConnection()
        scopeCompletionListener?.dispose()
    }

    class PurchaseListener : PurchasesUpdatedListener {
        var continuation: CancellableContinuation<List<Purchase>>? = null
            set(newEmitter) {
                continuation?.run {
                    if (!isCompleted) {
                        Timber.w("setPurchaseEmitter: previous operation is not completed")
                        resumeWithException(InterruptedException("Operation has been interrupted by another call"))
                    }
                }
                field = newEmitter
            }

        override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase?>?) {
            Timber.d("onPurchasesUpdated() called with: billingResult = %s; purchases = %s",
                    asString(billingResult), purchases)
            continuation?.run {
                if (isCompleted) {
                    return@run
                }
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    resume(purchases
                            ?.filterNotNull()
                            ?: emptyList())
                } else {
                    resumeWithException(PurchaseFailureException(billingResult))
                }
            }
        }
    }

    companion object {
        fun asString(billingResult: BillingResult?): String? {
            return if (billingResult == null) null else StringBuilder()
                    .append("BillingResult{")
                    .append("responseCode=").append(billingResult.responseCode)
                    .append(", debugMessage=").append(billingResult.debugMessage)
                    .append("}")
                    .toString()
        }
    }
}