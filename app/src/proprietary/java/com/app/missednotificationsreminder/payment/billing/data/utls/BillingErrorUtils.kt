package com.app.missednotificationsreminder.payment.billing.data.utls

import com.android.billingclient.api.BillingClient
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.data.source.remote.BillingOperationException
import com.app.missednotificationsreminder.payment.billing.domain.entities.BillingErrorCodes

/**
 * Handle billing error when detected
 **/
fun <T> Throwable?.handleBillingError(resourceDataSource: ResourceDataSource): ResultWrapper<T> {
    val error = this
    if (error is BillingOperationException) {
        when (error.code) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                return ResultWrapper.Error(error, BillingErrorCodes.BILLING_UNAVAILABLE, resourceDataSource.getString(R.string.payment_error_billing_unavailable))
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                return ResultWrapper.Error(error, BillingErrorCodes.SERVICE_UNAVAILABLE, resourceDataSource.getString(R.string.payment_error_service_unavailable))
            BillingClient.BillingResponseCode.USER_CANCELED ->
                return ResultWrapper.Error(error, BillingErrorCodes.USER_CANCELED, resourceDataSource.getString(R.string.payment_error_user_canceled))
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                return ResultWrapper.Error(error, BillingErrorCodes.ITEM_ALREADY_OWNED, resourceDataSource.getString(R.string.payment_error_purchase_pending))
        }
    }
    return ResultWrapper.Error(error)
}

fun <T> ResultWrapper.Error.handleBillingError(resourceDataSource: ResourceDataSource): ResultWrapper<T> {
    return throwable.handleBillingError(resourceDataSource)
}
