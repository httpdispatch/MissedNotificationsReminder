package com.app.missednotificationsreminder.settings

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.FragmentSettingsBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils
import com.app.missednotificationsreminder.settings.applicationssettings.ApplicationsSettingsViewModel
import com.app.missednotificationsreminder.settings.applicationssettings.ApplicationsSettingsViewStatePartialChanges
import com.app.missednotificationsreminder.settings.reminder.ReminderFragment
import com.app.missednotificationsreminder.settings.scheduler.SchedulerFragment
import com.app.missednotificationsreminder.settings.sound.SoundFragment
import com.app.missednotificationsreminder.settings.vibration.VibrationFragment
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel
import com.app.missednotificationsreminder.util.BatteryUtils
import com.jakewharton.u2020.data.LumberYard
import com.jakewharton.u2020.ui.logs.LogsDialog
import dagger.Binds
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays other settings view
 */
@ExperimentalCoroutinesApi
@FlowPreview
class SettingsFragment : CommonFragmentWithViewModel<SettingsViewModel?>() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val applicationsSettingsModel by viewModels<ApplicationsSettingsViewModel> { viewModelFactory }

    @Inject
    lateinit var _model: SettingsViewModel

    @Inject
    lateinit var lumberYard: LumberYard
    lateinit var mBinding: FragmentSettingsBinding

    override fun getModel(): SettingsViewModel {
        return _model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        // Set the lifecycle owner to the lifecycle of the view
        mBinding.lifecycleOwner = this.viewLifecycleOwner

        mBinding.model = model
        mBinding.applicationsSettingsViewState = applicationsSettingsModel.viewState.asLiveData()
        mBinding.fragment = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding.model = null
        mBinding.applicationsSettingsViewState = null
        mBinding.fragment = null
    }

    override fun onResume() {
        super.onResume()
        model.checkServiceEnabled(activity)
        model.checkBatteryOptimizationDisabled(activity)
        model.checkPermissions(activity)
        model.checkVibrationAvailable()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_settings_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.advanceSettingsVisible).isChecked = model.advancedSettingsVisible.get()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.advanceSettingsVisible) {
            // toggle menu item state and the related data binding value
            item.isChecked = !item.isChecked
            model.advancedSettingsVisible.set(item.isChecked)
            true
        } else if (item.itemId == R.id.showLog) {
            LogsDialog(ContextThemeWrapper(context, R.style.AppTheme), lumberYard).show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }


    /**
     * Method which is called when the select applications button is clicked. It launches the
     * [applications selection activity][ApplicationsSelectionFragment]
     *
     * @param v
     */
    fun onSelectApplicationsButtonClicked() {
        findNavController().navigate(ActionOnlyNavDirections(R.id.action_settingsFragment_to_applicationsSelectionFragment))
    }

    fun ignorePersistentNotificationsChanged(checked: Boolean) =
            applicationsSettingsModel.process(ApplicationsSettingsViewStatePartialChanges.IgnorePersistentNotificationsChange(checked))

    fun respectPhoneCallsChanged(checked: Boolean) =
            applicationsSettingsModel.process(ApplicationsSettingsViewStatePartialChanges.RespectPhoneCallsChange(checked))

    fun respectRingerModeChanged(checked: Boolean) =
            applicationsSettingsModel.process(ApplicationsSettingsViewStatePartialChanges.RespectRingerModeChange(checked))

    fun remindWhenScreenIsOnChanged(checked: Boolean) =
            applicationsSettingsModel.process(ApplicationsSettingsViewStatePartialChanges.RemindWhenScreenIsOnChange(checked))

    /**
     * Method which is called when the manage access button is clicked. It launches the system
     * notification listener settings window
     *
     * @param v
     */
    fun onManageBatteryOptimizationPressed(v: View?) {
        try {
            startActivity(BatteryUtils.getBatteryOptimizationIntent(context))
        } catch (ex: ActivityNotFoundException) {
            // possibly Oppo phone
            Timber.e(ex)
            // TODO notify view
        }
    }


    /**
     * Method which is called when the manage access button is clicked. It launches the system
     * notification listener settings window
     *
     * @param v
     */
    fun onManageAccessButtonPressed(v: View?) {
        startActivity(ReminderNotificationListenerServiceUtils.getServiceEnabledManagementIntent())
    }

    /**
     * Method which is called when the grant permissions button is clicked. It launches the grant permission dialog
     *
     * @param v
     */
    fun onGrantPermissionsPressed(v: View?) {
        Timber.d("onGrantPermissionsPressed")
        model.grantRequiredPermissions(activity)
    }

    @dagger.Module
    abstract class Module {
        @FragmentScope
        @ContributesAndroidInjector(
                modules = [
                    ModuleExt::class,
                    ReminderFragment.Module::class,
                    SchedulerFragment.Module::class,
                    SoundFragment.Module::class,
                    VibrationFragment.Module::class])
        abstract fun contribute(): SettingsFragment


        @FlowPreview
        @Binds
        @IntoMap
        @ViewModelKey(ApplicationsSettingsViewModel::class)
        internal abstract fun bindViewModel(viewmodel: ApplicationsSettingsViewModel): ViewModel
    }

    @dagger.Module
    class ModuleExt {
    }
}