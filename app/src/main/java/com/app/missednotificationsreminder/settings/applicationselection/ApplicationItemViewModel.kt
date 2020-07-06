package com.app.missednotificationsreminder.settings.applicationselection

import android.view.View
import com.app.missednotificationsreminder.binding.model.BaseViewModel
import com.app.missednotificationsreminder.binding.util.BindableBoolean
import com.app.missednotificationsreminder.binding.util.RxBindingUtils
import com.app.missednotificationsreminder.settings.applicationselection.data.model.ApplicationItem
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import timber.log.Timber

/**
 * The view model for the single application item
 *
 * @property applicationItem                        the current application item
 * @property picasso
 * @property applicationCheckedStateChangedListener the listener to subscribe to the on checked
 * state changed event
 */
class ApplicationItemViewModel(
        private val applicationItem: ApplicationItem,
        private val picasso: Picasso,
        private val applicationCheckedStateChangedListener: ApplicationCheckedStateChangedListener) : BaseViewModel() {
    /**
     * Data binding field to store application checked state
     */
    val checked = BindableBoolean()

    /**
     * Get the application name
     *
     * @return
     */
    val name: CharSequence
        get() {
            Timber.d("getName for %1\$s", toString())
            return applicationItem.applicationName
        }

    /**
     * Get the application description
     *
     * @return
     */
    val description: String
        get() {
            Timber.d("getDescription for %1\$s", toString())
            return applicationItem.packageName
        }

    /**
     * Get the application icon request
     *
     * @return
     */
    val icon: RequestCreator
        get() {
            Timber.d("getIcon for %1\$s", toString())
            return picasso.load(applicationItem.iconUri)
                    .fit()
        }

    fun hasActiveNotifications(): Boolean {
        return applicationItem.activeNotifications > 0
    }

    fun activeNotifications(): String {
        return applicationItem.activeNotifications.toString()
    }

    /**
     * Reverse checked state. Called when the application item clicked. Method binded directly in
     * the layout xml
     *
     * @param v
     */
    fun onItemClicked(v: View?) {
        Timber.d("onItemClicked for %1\$s", toString())
        checked.set(!checked.get())
    }

    override fun toString(): String {
        return String.format("%1\$s(checked=%2\$b, package=%3\$s)", javaClass.kotlin.simpleName, checked.get(), applicationItem.packageName)
    }

    init {
        Timber.d("Constructor")
        checked.set(applicationItem.checked)
        monitor(RxBindingUtils
                .valueChanged(checked)
                .skip(1) // skip the current value processing, which is passed automatically
                .subscribe { value: Boolean ->
                    Timber.d("Checked property changed for %1\$s", toString())
                    this.applicationCheckedStateChangedListener.onApplicationCheckedStateChanged(this.applicationItem, value)
                })
    }
}

/**
 * The interface subscribers to the onApplicationCheckedStateChanged event should implement
 */
interface ApplicationCheckedStateChangedListener {
    fun onApplicationCheckedStateChanged(applicationItem: ApplicationItem, checked: Boolean)
}