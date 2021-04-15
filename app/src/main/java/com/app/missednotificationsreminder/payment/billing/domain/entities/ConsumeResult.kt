package com.app.missednotificationsreminder.payment.billing.domain.entities

import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper

data class ConsumeResult(val skuDetails: List<SkuDetails>, val operationStatus: ResultWrapper<Unit>)
