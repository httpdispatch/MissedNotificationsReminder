package com.app.missednotificationsreminder.payment

import com.app.missednotificationsreminder.binding.model.BaseViewStateViewEffectModel
import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

/**
 * The view model for the single purchase item
 */
class PurchaseItemViewModel(purchaseItem: PurchaseItem) : BaseViewStateViewEffectModel<
        PurchaseItemViewState,
        PurchaseViewEffect,
        ViewStatePartialChanges<PurchaseItemViewState>>(PurchaseItemViewState(purchaseItem.skuDetails))