package com.app.missednotificationsreminder.payment.billing.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.common.domain.entities.*
import com.app.missednotificationsreminder.data.*
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.data.mappers.toDomain
import com.app.missednotificationsreminder.payment.billing.data.mappers.toRemote
import com.app.missednotificationsreminder.payment.billing.data.source.remote.BillingOperationException
import com.app.missednotificationsreminder.payment.billing.data.source.remote.CoroutinesBilling
import com.app.missednotificationsreminder.payment.billing.data.utls.collectWithLastErrorOrSuccessStatus
import com.app.missednotificationsreminder.payment.billing.data.utls.collectWithLastErrorOrSuccessStatusSimple
import com.app.missednotificationsreminder.payment.billing.data.utls.handleBillingError
import com.app.missednotificationsreminder.payment.billing.domain.entities.*
import com.app.missednotificationsreminder.payment.billing.domain.repository.PurchaseRepository
import com.app.missednotificationsreminder.util.coroutines.retryCallOnError
import com.app.missednotificationsreminder.util.loadingstate.BasicLoadingStateManager
import com.app.missednotificationsreminder.util.loadingstate.HasLoadingStateManager
import com.app.missednotificationsreminder.util.loadingstate.LoadingStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext

class PurchaseRepositoryImpl(
        scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
        private val resourceDataSource: ResourceDataSource,
        context: Context
) : HasLoadingStateManager, PurchaseRepository {
    val billing by lazy { CoroutinesBilling(scope, context) }
    override val loadingStateManager: LoadingStateManager by lazy { BasicLoadingStateManager() }

    private suspend fun <T> processBillingCall(
            operationName: String,
            maxRetryCount: Int,
            block: suspend () -> T
    ): ResultWrapper<T> {
        return attachLoading(operationName) {
            retryCallOnError(maxRetryCount) {
                block()
            }.onErrorReturn { error ->
                error.handleBillingError(resourceDataSource)
            }
        }
    }

    /**
     * Get the sku details for the specified [skuList] of the [skuType] product type
     */
    override suspend fun getSkuDetails(
            skuList: List<String>,
            skuType: SkuType
    ): ResultWrapper<List<SkuDetails>> {
        Timber.d(
                "getSkuDetails() called with: skuList = %s; skuType = %s",
                skuList, skuType
        )

        return attachLoading("getSkuListDetails") {
            attachLoadingStatus("Loading tariffs information") {
                skuList.asFlow()
                        .buffer()
                        .map {
                            processBillingCall(
                                    "getSkuDetails",
                                    2
                            ) { billing.getSkuDetails(listOf(it), skuType.toRemote()) }
                        }
                        .collectWithLastErrorOrSuccessStatusSimple(ResultWrapper.Success(emptyList())) { acc, value ->
                            acc + value.map { it.toDomain() }
                        }
            }
        }
    }

    /**
     * Launch the purchase flow for the specified product details
     */
    override suspend fun purchase(
            skuDetails: SkuDetails,
            oldSku: String?,
            oldPurchaseToken: String?,
            userId: String?,
            activity: Activity
    ): ResultWrapper<List<Purchase>> {
        Timber.d(
                "purchase() called with: skuDetails = %s; oldSku = %s; oldPurchaseToken = %s; userId = %s; activity = %s",
                skuDetails, oldSku, oldPurchaseToken, userId, activity
        )
        return processBillingCall("purchase", 0) {
            try {
                val remoteSkuDetails =
                        billing.getSkuDetails(listOf(skuDetails.sku), skuDetails.skuType.toRemote())
                billing.purchase(
                        remoteSkuDetails.first(),
                        oldSku,
                        oldPurchaseToken,
                        userId,
                        activity
                ).map { it.toDomain() }
            } catch (t: Throwable) {
                if (oldSku != null &&
                        t is BillingOperationException &&
                        t.code == BillingClient.BillingResponseCode.DEVELOPER_ERROR
                ) {
                    val remoteSkuDetails =
                            billing.getSkuDetails(listOf(skuDetails.sku), skuDetails.skuType.toRemote())
                    // Workaround for DEVELOPER_ERROR which may happen when old purchase
                    // has PURCHASED state but has been refunded or user is purchasing already purchased item
                    billing.purchase(
                            remoteSkuDetails.first(),
                            null,
                            null,
                            userId,
                            activity
                    ).map { it.toDomain() }
                } else {
                    throw t
                }
            }
        }
    }

    /**
     * Query purchases for the specified [skuType]
     */
    override suspend fun queryPurchases(skuType: SkuType): ResultWrapper<List<Purchase>> {
        Timber.d(
                "queryPurchases() called with: skuType = %s",
                skuType
        )
        return processBillingCall("purchase", 2) {
            billing.queryPurchases(skuType.toRemote())
                    .map { it.toDomain() }
        }
    }

    /**
     * Acknowledge [purchase] to avoid purchase transaction rollback
     */
    override suspend fun acknowledgePurchase(purchase: Purchase): ResultWrapper<Boolean> {
        Timber.d(
                "acknowledgePurchase() called with: purchase = %s",
                purchase
        )
        return processBillingCall("acknowledgePurchase", 2) {
            billing.acknowledgePurchase(purchase.purchaseToken)
            true
        }
    }

    /**
     * Consume the [purchase] so user may but it again
     */
    override suspend fun consumePurchase(purchase: Purchase): ResultWrapper<String> {
        Timber.d(
                "consumePurchase() called with: purchase = %s",
                purchase
        )
        return processBillingCall("consumePurchase", 2) {
            billing.consumePurchase(purchase.purchaseToken)
        }
    }

    /**
     * Check whether there are any unhandled purchases and try to handle them
     */
    override suspend fun verifyAndConsumePendingPurchases(): ConsumeResult {
        Timber.d("verifyAndConsumePendingPurchases() called")
        return attachLoading("verifyAndConsumePendingPurchases") {
            attachLoadingStatus("Verifying and consuming pending purchases") {
                flowOf(SkuType.INAPP, SkuType.SUBS)
                        .map { skuType ->
                            queryPurchases(skuType)
                                    .map {
                                        acknowledgePurchases(it, skuType == SkuType.INAPP, skuType)
                                    }
                        }
                        .collectWithLastErrorOrSuccessStatus(ConsumeResult(
                                emptyList(),
                                ResultWrapper.Success(Unit)
                        ),
                                { it is ResultWrapper.Success && it.data.operationStatus.succeededOrPurchasePending() }) { acc, value ->
                            value.fold({ consumeResult ->
                                with(acc) {
                                    copy(
                                            skuDetails = skuDetails + consumeResult.skuDetails,
                                            operationStatus = operationStatus.fold(
                                                    // if accumulated operation status is success
                                                    // use last received operation status as accumulated
                                                    { consumeResult.operationStatus },
                                                    // else don't overwrite accumulated operation status
                                                    { it })
                                    )
                                }
                            }, { error ->
                                with(acc) {
                                    // calculate new operation status for accumulated value
                                    copy(
                                            operationStatus = operationStatus.fold(
                                                    // if accumulated operation status is success
                                                    // use last received operation status as accumulated
                                                    { error },
                                                    // else don't overwrite accumulated operation status
                                                    { operationStatus })
                                    )
                                }
                            })
                        }
            }
        }
    }

    private suspend fun acknowledgePurchases(
            purchases: List<Purchase>,
            consumable: Boolean,
            skuType: SkuType
    ): ConsumeResult {
        return purchases.asFlow()
                .filter { !it.isAcknowledged || consumable }
                .flatMapMerge(concurrency = 1) { purchase ->
                    flow {
                        when (purchase.purchaseState) {
                            PurchaseState.PURCHASED -> {
                                acknowledgePurchase(purchase)
                                        .flatMap {
                                            consumePurchase(purchase)
                                                    .flatMap { getSkuDetails(listOf(purchase.sku), skuType) }
                                        }
                                        .also { emit(it) }
                            }
                            PurchaseState.PENDING ->
                                emit(
                                        ResultWrapper.Error(
                                                code = BillingErrorCodes.PURCHASE_PENDING,
                                                message = resourceDataSource.getString(R.string.payment_error_purchase_pending)
                                        )
                                )
                            else -> {
                            }// do nothing
                        }
                    }
                }
                .collectWithLastErrorOrSuccessStatus(ConsumeResult(
                        emptyList(),
                        ResultWrapper.Success(Unit)
                ),
                        { it.succeededOrPurchasePending() }) { acc, value ->
                    value.fold({
                        acc.copy(skuDetails = acc.skuDetails + it)
                    }, { error ->
                        acc.operationStatus.fold({ acc.copy(operationStatus = error) }, { acc })
                    })
                }
    }

}

private fun <R> ResultWrapper<R>.succeededOrPurchasePending(): Boolean {
    val rw = this@succeededOrPurchasePending
    return rw.succeeded ||
            (rw is ResultWrapper.Error && rw.code == BillingErrorCodes.PURCHASE_PENDING)
}
