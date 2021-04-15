package com.app.missednotificationsreminder.payment

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.BaseViewStateViewEffectModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.common.domain.entities.*
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.domain.entities.BillingErrorCodes
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuDetails
import com.app.missednotificationsreminder.payment.billing.domain.entities.SkuType
import com.app.missednotificationsreminder.payment.billing.domain.repository.PurchaseRepository
import com.app.missednotificationsreminder.payment.di.qualifiers.AvailableSkus
import com.app.missednotificationsreminder.payment.model.Purchase
import com.app.missednotificationsreminder.util.loadingstate.HasLoadingStateManager
import com.app.missednotificationsreminder.util.loadingstate.LoadingState
import com.app.missednotificationsreminder.util.loadingstate.LoadingStateManager
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

/**
 * The view model for the applications selection view
 */
class PurchaseViewModel @Inject constructor(
        @param:AvailableSkus private val skus: List<String>,
        private val purchaseRepository: PurchaseRepository,
        private val purchases: Preference<List<Purchase>>,
        private val resourcesDataSource: ResourceDataSource,
) :
        BaseViewStateViewEffectModel<PurchaseViewState, PurchaseViewEffect, PurchaseViewStatePartialChanges>(
                PurchaseViewState(contributeOptions = resourcesDataSource.getString(R.string.contribution_contribute_options))
        ),
        ObservesPendingPayments by ObservesPendingPaymentsImpl(purchaseRepository, purchases),
        HasLoadingStateManager {
    init {
        viewModelScope.launch {
            _viewState.bindWithPreferences(purchases,
                    { newValue, vs ->
                        PurchaseViewStatePartialChanges.PurchasesUpdated(newValue, resourcesDataSource).reduce(vs)
                    },
                    { purchases.get() })
        }
    }

    override val loadingStateManager: LoadingStateManager by lazy {
        object : LoadingStateManager() {
            override var loadingState: LoadingState
                get() = viewState.value.loadingState
                set(value) {
                    processSync(PurchaseViewStatePartialChanges.LoadingStateChange(value))
                }

        }
    }

    /**
     * Load the purchase item data to the view
     */
    fun loadData() {
        viewModelScope.launch {
            Timber.d("loadData: thread=%s", Thread.currentThread().name)
            if (viewState.value.loadingState.loading) {
                Timber.d("loadData: already loading, return")
                return@launch
            }
            if (viewState.value.data?.succeeded == true) {
                Timber.d("loadData: already loaded, return")
                return@launch
            }
            attachLoading("loadData") {
                attachLoadingStatus(resourcesDataSource.getString(R.string.payment_loading_purchase_items)) {
                    purchaseRepository.verifyAndConsumePendingPurchases()
                            .also { purchaseCompleted(it.skuDetails) }
                    purchaseRepository.getSkuDetails(skus, SkuType.INAPP)
                            .map { skuDetails ->
                                skuDetails
                                        .asSequence()
                                        .sortedBy { it.priceAmountMicros }
                                        .map { PurchaseItem(it) }
                                        .toList()
                            }
                            .let {
                                processSync(PurchaseViewStatePartialChanges.DataStateChange(it))
                            }
                }
            }
        }
    }

    fun purchase(skuDetails: SkuDetails, activity: Activity) {
        viewModelScope.launch {
            if (viewState.value.loadingState.loading) {
                Timber.d("purchase: already loading, return")
                return@launch
            }
            attachLoading("purchase") {
                attachLoadingStatus(resourcesDataSource.getString(R.string.payment_purchasing)) {
                    purchaseRepository.purchase(skuDetails = skuDetails, activity = activity)
                            .flatMap {
                                purchaseRepository.verifyAndConsumePendingPurchases()
                                        .also { purchaseCompleted(it.skuDetails) }
                                        .operationStatus
                            }
                            .fold(
                                    onSuccess = {
                                        requestViewEffect(
                                                PurchaseViewEffect.Message(
                                                        resourcesDataSource.getString(
                                                                R.string.payment_purchase_done
                                                        )
                                                )
                                        )
                                    },
                                    onFailure = { error ->
                                        if (canRetryPendingPayments(error)) {
                                            viewModelScope.launch { observePendingPayments() }
                                        }
                                        error.messageOrDefault { resourcesDataSource.getString(R.string.common_general_error) }
                                                .also { requestViewEffect(PurchaseViewEffect.Message(it)) }
                                    })
                }
            }
        }
    }
}

@ExperimentalTime
interface ObservesPendingPayments {
    suspend fun observePendingPayments(
            initialDelay: Long = DurationUnit.MINUTES.toMillis(3),
            interval: Long = DurationUnit.MINUTES.toMillis(3)
    )

    fun canRetryPendingPayments(error: ResultWrapper.Error): Boolean
    fun purchaseCompleted(purchasedGoods: List<SkuDetails>)
}

@ExperimentalTime
class ObservesPendingPaymentsImpl(
        private val purchaseRepository: PurchaseRepository,
        private val purchases: Preference<List<Purchase>>
) : ObservesPendingPayments {
    override suspend fun observePendingPayments(initialDelay: Long, interval: Long) {
        Timber.d("observePendingPayments() called with: initialDelay = $initialDelay, interval = $interval")
        delay(initialDelay)
        while (true) {
            purchaseRepository.verifyAndConsumePendingPurchases()
                    .also { purchaseCompleted(it.skuDetails) }
                    .operationStatus
                    .fold({
                        return@observePendingPayments
                    }, { error ->
                        if (!canRetryPendingPayments(error)) {
                            return@observePendingPayments
                        }
                    })
            delay(DurationUnit.MINUTES.toMillis(3))
        }
    }

    override fun canRetryPendingPayments(error: ResultWrapper.Error): Boolean {
        Timber.d("canRetryPendingPayments() called with: error = $error")
        return setOf(BillingErrorCodes.PURCHASE_PENDING, BillingErrorCodes.SERVICE_UNAVAILABLE)
                .any { error.code == it }
    }

    override fun purchaseCompleted(purchasedGoods: List<SkuDetails>) {
        Timber.d("purchaseCompleted() called with: purchasedGoods = $purchasedGoods")
        purchasedGoods
                .map { Purchase(it.sku, it.price) }
                .takeIf { it.isNotEmpty() }
                ?.run { purchases.set(purchases.get() + this@run) }
    }
}
