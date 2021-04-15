package com.app.missednotificationsreminder.payment.billing.data

import android.app.Activity
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.domain.entities.*
import com.app.missednotificationsreminder.payment.billing.domain.repository.PurchaseRepository

class PurchaseRepositoryImpl(
        resourceDataSource: ResourceDataSource
) : PurchaseRepository {

    private val defaultAnswer = ResultWrapper.Error(
            throwable = null,
            code = BillingErrorCodes.BILLING_UNAVAILABLE,
            message = resourceDataSource.getString(R.string.payment_error_billing_unavailable)
    )

    override suspend fun getSkuDetails(
            skuList: List<String>,
            skuType: SkuType
    ): ResultWrapper<List<SkuDetails>> {
        return defaultAnswer
    }

    override suspend fun purchase(
            skuDetails: SkuDetails,
            oldSku: String?,
            oldPurchaseToken: String?,
            userId: String?,
            activity: Activity
    ): ResultWrapper<List<Purchase>> {
        return defaultAnswer
    }

    override suspend fun queryPurchases(skuType: SkuType): ResultWrapper<List<Purchase>> {
        return defaultAnswer
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): ResultWrapper<Boolean> {
        return defaultAnswer
    }

    override suspend fun consumePurchase(purchase: Purchase): ResultWrapper<String> {
        return defaultAnswer
    }

    override suspend fun verifyAndConsumePendingPurchases(): ConsumeResult {
        return ConsumeResult(skuDetails = emptyList(), operationStatus = defaultAnswer)
    }
}
