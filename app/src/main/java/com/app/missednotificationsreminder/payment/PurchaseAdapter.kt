package com.app.missednotificationsreminder.payment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.app.missednotificationsreminder.data.onSuccess
import com.app.missednotificationsreminder.databinding.ItemContributeBinding
import com.app.missednotificationsreminder.databinding.ItemDonateBinding
import com.app.missednotificationsreminder.ui.widget.recyclerview.LifecycleAdapterWithViewEffect
import com.app.missednotificationsreminder.ui.widget.recyclerview.LifecycleViewHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * [RecyclerView] adapter to provide purchase functionality in the
 * contribution window
 */
@ExperimentalCoroutinesApi
class PurchaseAdapter @Inject constructor(private val purchaseViewState: StateFlow<PurchaseViewState>) : LifecycleAdapterWithViewEffect<LifecycleViewHolder, PurchaseViewEffect>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val data = mutableListOf<PurchaseItem>()

    init {
        purchaseViewState
                .map { it.data }
                .filterNotNull()
                .distinctUntilChanged()
                .onEach { it.onSuccess(::setData) }
                .launchIn(lifecycleScope)
    }

    private fun setData(data: List<PurchaseItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): LifecycleViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemContributeBinding.inflate(inflater, viewGroup, false)
                HeaderViewHolder(binding)
            }
            TYPE_ITEM -> {
                val binding = ItemDonateBinding.inflate(inflater, viewGroup, false)
                ViewHolder(binding)
            }
            else -> throw RuntimeException("There are no type that matches $viewType")
        }

    }

    override fun onBindViewHolder(viewHolder: LifecycleViewHolder, position: Int) {
        when(viewHolder){
            is ViewHolder -> viewHolder.bindTo(getItem(position))
            is HeaderViewHolder -> viewHolder.bindTo(purchaseViewState)
        }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isPositionHeader(position)) TYPE_HEADER else TYPE_ITEM
    }

    private fun isPositionHeader(position: Int): Boolean {
        return position == 0
    }

    private fun getItem(position: Int): PurchaseItem {
        return data[position - 1]
    }

    /**
     * View holder implementation for this adapter
     */
    inner class ViewHolder(var binding: ItemDonateBinding) : LifecycleViewHolder(binding.root, lifecycle) {
        lateinit var model: PurchaseItemViewModel

        init {
            binding.lifecycleOwner = this@ViewHolder
        }

        fun bindTo(item: PurchaseItem) {
            model = PurchaseItemViewModel(item)
                    .also {
                        binding.viewState = it.viewState.asLiveData()
                    }
            binding.interactor = object : PurchaseInteractor {
                override fun purchase() {
                    requestViewEffect(PurchaseViewEffect.Purchase(item.skuDetails))
                }
            }
        }
    }

    /**
     * Header view holder implementation for this adapter
     */
    inner class HeaderViewHolder(var binding: ItemContributeBinding) : LifecycleViewHolder(binding.root, lifecycle) {
        init {
            binding.lifecycleOwner = this@HeaderViewHolder
        }

        fun bindTo(item: StateFlow<PurchaseViewState>) {
            binding.viewState = item.asLiveData()
        }
    }
}