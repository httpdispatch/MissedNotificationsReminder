package com.app.missednotificationsreminder.settings

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.model.NightMode
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
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import com.app.missednotificationsreminder.util.BatteryUtils
import com.jakewharton.u2020.data.LumberYard
import com.jakewharton.u2020.ui.logs.LogsDialog
import dagger.Binds
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays other settings view
 */
@ExperimentalCoroutinesApi
@FlowPreview
class SettingsFragment : CommonFragmentWithViewBinding<FragmentSettingsBinding>(R.layout.fragment_settings) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val applicationsSettingsModel by viewModels<ApplicationsSettingsViewModel> { viewModelFactory }
    private val viewModel by viewModels<SettingsViewModel> { viewModelFactory }

    @Inject
    lateinit var lumberYard: LumberYard

    private var scrollPosition: Int = 0

    private val grantRequiredPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                result.filterValues { false }
                        .keys
                        .joinToString(", ")
                        .run { viewModel.process(SettingsViewStatePartialChanges.MissingPermissionsChanged(this)) }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        viewDataBinding.apply {
            // Set the lifecycle owner to the lifecycle of the view
            lifecycleOwner = viewLifecycleOwner
            fragment = this@SettingsFragment
            viewModel = this@SettingsFragment.viewModel
            viewState = this@SettingsFragment.viewModel.viewState.asLiveData()
            applicationsSettingsViewState = applicationsSettingsModel.viewState.asLiveData()
            lifecycleScope.launchWhenResumed { scroll.scrollTo(0, scrollPosition) }
        }
        viewModel.viewState
                .map { it.nightMode }
                .distinctUntilChanged()
                .onEach { AppCompatDelegate.setDefaultNightMode(it.nightModeId) }
                .launchIn(lifecycleScope)
    }

    override fun onDestroyView() {
        scrollPosition = viewDataBinding.scroll.scrollY
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkServiceEnabled(requireActivity())
        viewModel.checkBatteryOptimizationDisabled(requireActivity())
        viewModel.checkPermissions(requireActivity())
        viewModel.checkVibrationAvailable()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_settings_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.advanced_settings_visible).isChecked = viewModel.viewState.value.advancedSettingsVisible
        when (viewModel.viewState.value.nightMode) {
            NightMode.FOLLOW_SYSTEM -> menu.findItem(R.id.night_mode_follow_system).isChecked = true
            NightMode.YES -> menu.findItem(R.id.night_mode_yes).isChecked = true
            NightMode.NO -> menu.findItem(R.id.night_mode_no).isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.advanced_settings_visible -> {
                // toggle menu item state and the related data binding value
                item.isChecked = !item.isChecked
                viewModel.process(SettingsViewStatePartialChanges.AdvancedSettingsVisibleChanged(item.isChecked))
                true
            }
            R.id.show_log -> {
                LogsDialog(ContextThemeWrapper(context, R.style.AppTheme), lumberYard, this).show()
                true
            }
            R.id.night_mode_follow_system -> {
                viewModel.process(SettingsViewStatePartialChanges.NightModeChanged(NightMode.FOLLOW_SYSTEM))
                true
            }
            R.id.night_mode_yes -> {
                viewModel.process(SettingsViewStatePartialChanges.NightModeChanged(NightMode.YES))
                true
            }
            R.id.night_mode_no -> {
                viewModel.process(SettingsViewStatePartialChanges.NightModeChanged(NightMode.NO))
                true
            }
            R.id.contribute -> {
                findNavController()
                        .navigate(ActionOnlyNavDirections(R.id.action_settingsFragment_to_contributionFragment))
                true
            }
            R.id.rate_app -> {
                viewModel.rateApp(requireActivity())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    /**
     * Method which is called when the select applications button is clicked. It launches the
     * applications selection fragment
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
        grantRequiredPermissions.launch(SettingsViewModel.REQUIRED_PERMISSIONS.toTypedArray())
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
        @ViewModelKey(SettingsViewModel::class)
        internal abstract fun bindViewModel(viewModel: SettingsViewModel): ViewModel

        @FlowPreview
        @Binds
        @IntoMap
        @ViewModelKey(ApplicationsSettingsViewModel::class)
        internal abstract fun bindApplicationSettingsViewModel(viewModel: ApplicationsSettingsViewModel): ViewModel
    }

    @dagger.Module
    class ModuleExt {

        @Provides
        fun provideSettingsViewState(fragment: SettingsFragment): LiveData<SettingsViewState> {
            return fragment.viewModel.viewState.asLiveData()
        }
    }
}