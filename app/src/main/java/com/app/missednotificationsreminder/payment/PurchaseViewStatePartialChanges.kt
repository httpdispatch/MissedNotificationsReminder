package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges
import com.app.missednotificationsreminder.data.ResultWrapper
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.util.loadingstate.LoadingState
import com.app.missednotificationsreminder.payment.data.model.Purchase

sealed class PurchaseViewStatePartialChanges : ViewStatePartialChanges<PurchaseViewState> {
    data class LoadingStateChange(
            private val newValue: LoadingState) : PurchaseViewStatePartialChanges() {
        override fun reduce(previousState: PurchaseViewState): PurchaseViewState {
            return previousState.copy(loadingState = newValue)
        }
    }

    data class DataStateChange(
            private val newValue: ResultWrapper<List<PurchaseItem>>) : PurchaseViewStatePartialChanges() {
        override fun reduce(previousState: PurchaseViewState): PurchaseViewState {
            return previousState.copy(data = newValue)
        }
    }

    data class PurchasesUpdated(
            private val newValue: List<Purchase>, private val resourceDataSource: ResourceDataSource) : PurchaseViewStatePartialChanges() {
        override fun reduce(previousState: PurchaseViewState): PurchaseViewState {
            return previousState.copy(purchases = resourceDataSource.getString(
                    R.string.payment_contributions,
                    newValue.joinToString(", ") { it.price }))
        }
    }
}