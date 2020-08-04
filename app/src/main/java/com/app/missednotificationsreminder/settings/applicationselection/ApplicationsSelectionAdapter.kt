package com.app.missednotificationsreminder.settings.applicationselection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.app.missednotificationsreminder.databinding.ItemSelectableApplicationBinding
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.ui.widget.recyclerview.LifecycleAdapter
import com.app.missednotificationsreminder.ui.widget.recyclerview.LifecycleViewHolder
import com.squareup.picasso.Picasso
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import javax.inject.Inject

/**
 * [RecyclerView] adapter to provide applications selection functionality in the
 * [applications selection view][ApplicationsSelectionFragment]
 */
@ExperimentalCoroutinesApi
class ApplicationsSelectionAdapter @Inject constructor(
        @param:SelectedApplications private val selectedApplications: Preference<Set<String>>,
        notificationDataFlow: Flow<@JvmSuppressWildcards List<NotificationData>>,
        private val picasso: Picasso) : LifecycleAdapter<ApplicationsSelectionAdapter.ViewHolder>() {
    private val subscription = CompositeSubscription()
    private val data = SortedList(ApplicationItemViewState::class.java, object : SortedListAdapterCallback<ApplicationItemViewState>(this) {
        override fun compare(t0: ApplicationItemViewState, t1: ApplicationItemViewState): Int {
            if (t0.activeNotifications != t1.activeNotifications) {
                return t1.activeNotifications - t0.activeNotifications
            }
            return if (t0.checked != t1.checked) {
                if (t0.checked) -1 else 1
            } else getLabel(t0).compareTo(getLabel(t1), ignoreCase = true)
        }

        fun getLabel(item: ApplicationItemViewState): String {
            return item.applicationName.toString()
        }

        override fun areContentsTheSame(oldItem: ApplicationItemViewState,
                                        newItem: ApplicationItemViewState): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(item1: ApplicationItemViewState, item2: ApplicationItemViewState): Boolean {
            return item1.packageName == item2.packageName
        }
    })

    fun setData(data: List<ApplicationItemViewState>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val binding = ItemSelectableApplicationBinding.inflate(inflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bindTo(data[i])
    }

    override fun getItemCount(): Int {
        return data.size()
    }

    fun shutdown() {
        subscription.clear()
    }

    /**
     * View holder implementation for this adapter
     */
    inner class ViewHolder(var binding: ItemSelectableApplicationBinding) : LifecycleViewHolder(binding.root, lifecycle) {
        var job: Job? = null
        lateinit var model: ApplicationItemViewModel

        init {
            binding.lifecycleOwner = this@ViewHolder
        }

        fun bindTo(item: ApplicationItemViewState) {
            model = ApplicationItemViewModel(item, picasso)
            binding.viewState = model.viewState.asLiveData()
            binding.model = model
            job = lifecycleScope.launchWhenCreated {
                try {
                    attachListeners(model)
                } finally {
                    Timber.d("ViewHolder job is canceled")
                }
            }
        }

        override fun onAttached() {
            super.onAttached()
            if (!job!!.isActive) {
                job = lifecycleScope.launchWhenCreated {
                    attachListeners(model)
                }
            }
        }

        private suspend fun attachListeners(model: ApplicationItemViewModel) {
            Timber.d("attachListeners")
            model.viewState
                    .drop(1)
                    .distinctUntilChanged { old, new -> old.checked == new.checked }
                    .onEach { applicationItem ->
                        data.updateItemAt(adapterPosition, applicationItem)
                        Timber.d("Update selected application value %1\$s to %2\$b", applicationItem.packageName, applicationItem.checked)
                        // for sure we may use if condition here instead of concatenation of 2 observables. Just wanted to achieve
                        // same result with RxJava usage.
                        selectedApplications.get()
                                .let {
                                    val updatedSet = it.toMutableSet()
                                    if (updatedSet.contains(applicationItem.packageName))
                                        updatedSet.remove(applicationItem.packageName)
                                    else
                                        updatedSet.add(applicationItem.packageName)
                                    selectedApplications.set(updatedSet.toSet())
                                }
                    }
                    .collect()
        }
    }

    companion object {
        fun getNotificationCountData(notificationData: List<NotificationData>): Map<String, Int> {
            return notificationData.groupingBy { it.packageName }
                    .eachCount()
        }
    }

    init {
        setHasStableIds(false)
        notificationDataFlow
                .conflate()
                .onEach { notificationData ->
                    val notificationsCountInfo = getNotificationCountData(notificationData)
                    while (true) {
                        var found = false
                        for (i in 0 until data.size()) {
                            val item = data[i]
                            val count = notificationsCountInfo[item.packageName] ?: 0
                            if (item.activeNotifications != count) {
                                data.updateItemAt(i, item.copy(activeNotifications = count))
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            break
                        }
                    }
                }
                .launchIn(lifecycleScope)
    }
}