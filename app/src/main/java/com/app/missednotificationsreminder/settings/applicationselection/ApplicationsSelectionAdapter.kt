package com.app.missednotificationsreminder.settings.applicationselection

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.app.missednotificationsreminder.databinding.ApplicationSelectableItemViewBinding
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.settings.applicationselection.data.model.ApplicationItem
import com.squareup.picasso.Picasso
import rx.Completable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * [RecyclerView] adapter to provide applications selection functionality in the
 * [applications selection view][ApplicationsSelectionFragment]
 *
 * @property checkedStateChangedListener the listener for the application checked/unchecked event
 * @property notificationDataObservable
 * @property packageManager
 * @property picasso
 */
class ApplicationsSelectionAdapter @Inject constructor(
        private val checkedStateChangedListener: ApplicationCheckedStateChangedListener,
        private val notificationDataObservable: Observable<List<NotificationData>>,
        private val packageManager: PackageManager,
        private val picasso: Picasso) : RecyclerView.Adapter<ApplicationsSelectionAdapter.ViewHolder>() {
    private val subscription = CompositeSubscription()
    private val data = SortedList(ApplicationItem::class.java, object : SortedListAdapterCallback<ApplicationItem>(this) {
        override fun compare(t0: ApplicationItem, t1: ApplicationItem): Int {
            if (t0.activeNotifications != t1.activeNotifications) {
                return t1.activeNotifications - t0.activeNotifications
            }
            return if (t0.checked != t1.checked) {
                if (t0.checked) -1 else 1
            } else getLabel(t0).compareTo(getLabel(t1), ignoreCase = true)
        }

        fun getLabel(item: ApplicationItem): String {
            return item.applicationName.toString()
        }

        override fun areContentsTheSame(oldItem: ApplicationItem,
                                        newItem: ApplicationItem): Boolean {
            return ObjectsCompat.equals(oldItem, newItem)
        }

        override fun areItemsTheSame(item1: ApplicationItem, item2: ApplicationItem): Boolean {
            return item1 === item2
        }
    })

    fun setData(data: List<ApplicationItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val binding = ApplicationSelectableItemViewBinding.inflate(inflater, viewGroup, false)
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
    inner class ViewHolder(var binding: ApplicationSelectableItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindTo(item: ApplicationItem) {
            binding.model = ApplicationItemViewModel(item,
                    picasso,
                    object : ApplicationCheckedStateChangedListener {
                        override fun onApplicationCheckedStateChanged(applicationItem: ApplicationItem, checked: Boolean) {
                            Timber.d("Update checked value to %1\$b", checked)
                            data.updateItemAt(adapterPosition, item.copy(checked = checked))
                            // notify global listener if exists
                            checkedStateChangedListener.onApplicationCheckedStateChanged(applicationItem, checked)
                        }
                    })
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
        subscription.add(notificationDataObservable
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .map { notificationData: List<NotificationData> -> getNotificationCountData(notificationData) }
                .flatMapCompletable { notificationsCountInfo: Map<String, Int> ->
                    Completable.fromAction {
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
                }
                .subscribe())
    }
}