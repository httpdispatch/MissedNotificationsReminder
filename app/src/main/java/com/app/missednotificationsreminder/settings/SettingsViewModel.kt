package com.app.missednotificationsreminder.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.data.model.NightMode
import com.app.missednotificationsreminder.payment.ObservesPendingPayments
import com.app.missednotificationsreminder.payment.ObservesPendingPaymentsImpl
import com.app.missednotificationsreminder.payment.billing.data.source.PurchaseRepository
import com.app.missednotificationsreminder.payment.data.model.Purchase
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils
import com.app.missednotificationsreminder.settings.di.qualifiers.ForceWakeLock
import com.app.missednotificationsreminder.settings.di.qualifiers.RateAppClicked
import com.app.missednotificationsreminder.util.BatteryUtils
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the settings view
 */
@ExperimentalCoroutinesApi
class SettingsViewModel @Inject constructor(private val vibrator: Vibrator,
                                            private val nightMode: Preference<NightMode>,
                                            @param:ForceWakeLock private val forceWakeLock: Preference<Boolean>,
                                            @param:RateAppClicked private val rateAppClicked: Preference<Boolean>,
                                            private val purchaseRepository: PurchaseRepository,
                                            private val purchases: Preference<List<Purchase>>) :
        BaseViewStateModel<SettingsViewState, SettingsViewStatePartialChanges>(SettingsViewState()),
        ObservesPendingPayments by ObservesPendingPaymentsImpl(purchaseRepository, purchases) {

    init {
        Timber.d("SettingsViewModel: init")
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(nightMode,
                        { newValue, vs ->
                            SettingsViewStatePartialChanges.NightModeChanged(newValue).reduce(vs)
                        },
                        { it.nightMode })
            }
            launch {
                _viewState.bindWithPreferences(forceWakeLock,
                        { newValue, vs ->
                            SettingsViewStatePartialChanges.ForceWakeLockChange(newValue).reduce(vs)
                        },
                        { it.forceWakeLock })
            }
            observePendingPayments(initialDelay = 0)
        }
    }

    /**
     * Run the operation to check whether the notification service is enabled
     */
    fun checkServiceEnabled(context: Context) {
        ReminderNotificationListenerServiceUtils.isServiceEnabled(context, ReminderNotificationListenerService::class.java)
                .run {
                    process(SettingsViewStatePartialChanges.AccessEnabledChange(this))
                }
    }

    /**
     * Run the operation to check whether the battery optimization is disabled for the application
     */
    fun checkBatteryOptimizationDisabled(context: Context) {
        (!viewState.value.isBatteryOptimizationSettingsVisible ||
                BatteryUtils.isBatteryOptimizationDisabled(context))
                .run { process(SettingsViewStatePartialChanges.BatteryOptimizationDisabledChanged(this)) }
    }

    /**
     * Check whether all required permissions are granted
     */
    fun checkPermissions(context: Context) {
        REQUIRED_PERMISSIONS
                .filter {
                    ContextCompat.checkSelfPermission(
                            context,
                            it
                    ) == PackageManager.PERMISSION_DENIED
                }
                .joinToString(", ")
                .run {
                    process(SettingsViewStatePartialChanges.MissingPermissionsChanged(this))
                }
    }

    /**
     * Check whether the vibration is allowed on device
     */
    fun checkVibrationAvailable() {
        vibrator.hasVibrator()
                .run { process(SettingsViewStatePartialChanges.VibrationSettingsAvailableChanged(this)) }
    }

    fun forceWakeLockChanged(value: Boolean) {
        process(SettingsViewStatePartialChanges.ForceWakeLockChange(value))
    }

    fun rateApp(activity: Activity) {
        rateAppClicked.set(true)
        val uri = Uri.parse("market://details?id=${activity.packageName}")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        try {
            activity.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
        }
    }

    companion object {
        /**
         * Permissions required by the application
         */
        val REQUIRED_PERMISSIONS = listOf(
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE)
    }
}