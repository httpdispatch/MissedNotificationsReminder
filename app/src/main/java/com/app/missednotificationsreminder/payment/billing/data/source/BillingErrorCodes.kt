package com.app.missednotificationsreminder.payment.billing.data.source

import com.android.billingclient.api.BillingClient
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.ResultWrapper
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.data.source.remote.BillingOperationException

object BillingErrorCodes {
    const val BILLING_UNAVAILABLE = BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
    const val SERVICE_UNAVAILABLE = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
    const val USER_CANCELED = BillingClient.BillingResponseCode.USER_CANCELED

    /**
     * The purchase payment is pending
     */
    const val PURCHASE_PENDING = 10

    /**
     * Handle billing error when detected
     *
     * @param error
     * @param <T>
     * @return
    </T> */
    fun <T> handleBillingError(error: Throwable?, resourceDataSource: ResourceDataSource): ResultWrapper<T> {
        if (error is BillingOperationException) {
            when (error.code) {
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                    return ResultWrapper.Error(error, BILLING_UNAVAILABLE, resourceDataSource.getString(R.string.payment_error_billing_unavailable))
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                    return ResultWrapper.Error(error, SERVICE_UNAVAILABLE, resourceDataSource.getString(R.string.payment_error_service_unavailable))
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    return ResultWrapper.Error(error, USER_CANCELED, resourceDataSource.getString(R.string.payment_error_user_canceled))
            }
        }
        return ResultWrapper.Error(error)
    }

    fun <T> ResultWrapper.Error.handleBillingError(resourceDataSource: ResourceDataSource): ResultWrapper<T> {
        return handleBillingError(throwable, resourceDataSource)
    }
}