package com.app.missednotificationsreminder.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils
import com.app.missednotificationsreminder.util.BatteryUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the settings view
 */
@ExperimentalCoroutinesApi
class SettingsViewModel @Inject constructor(private val vibrator: Vibrator) :
        BaseViewStateModel<SettingsViewState, SettingsViewStatePartialChanges>(SettingsViewState()) {

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

    companion object {
        /**
         * Permissions required by the application
         */
        val REQUIRED_PERMISSIONS = listOf(
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE)
    }

    init {
        Timber.d("SettingsViewModel: init")
    }
}