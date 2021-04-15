package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper
import com.app.missednotificationsreminder.util.loadingstate.LoadingState

data class PurchaseViewState(
        val loadingState: LoadingState = LoadingState(),
        val data: ResultWrapper<List<PurchaseItem>>? = null,
        val purchases: String = "",
        val contributeOptions: String
) {
    val error = if (data is ResultWrapper.Error) data.messageOrDefault { "Not specified" } else "None"
    val errorVisible = data is ResultWrapper.Error

    val purchasesVisible: Boolean
        get() = purchases.isNotEmpty()
}
