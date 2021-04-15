package com.app.missednotificationsreminder.payment.billing.data.source.remote

import com.android.billingclient.api.BillingResult

sealed class BillingOperationException(val code: Int, message: String) : Exception(message) {

    constructor(billingResult: BillingResult) : this(billingResult.responseCode, billingResult.debugMessage)

    override fun toString(): String {
        return StringBuilder()
                .append(getClassName() + "{")
                .append("code=").append(code)
                .append("}")
                .toString()
    }

    private fun getClassName() = this::class.simpleName
}

class AcknowledgePurchaseFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}

class ConnectionFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}

class ConsumePurchaseFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}

class PurchaseFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}

class QueryPurchaseFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}

class SkuDetailsFailureException : BillingOperationException {
    constructor(billingResult: BillingResult) : super(billingResult)
    constructor(code: Int, message: String) : super(code, message)
}
