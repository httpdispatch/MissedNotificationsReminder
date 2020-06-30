package com.app.missednotificationsreminder.ui.fragment

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.SettingsViewModel
import com.app.missednotificationsreminder.databinding.SettingsViewBinding
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel
import com.app.missednotificationsreminder.util.BatteryUtils
import com.jakewharton.u2020.data.LumberYard
import com.jakewharton.u2020.ui.logs.LogsDialog
import dagger.android.ContributesAndroidInjector
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays other settings view
 *
 * @author Eugene Popovich
 */
class SettingsFragment : CommonFragmentWithViewModel<SettingsViewModel?>() {
    @Inject
    lateinit var _model: SettingsViewModel

    @Inject
    lateinit var lumberYard: LumberYard
    lateinit var mBinding: SettingsViewBinding

    override fun getModel(): SettingsViewModel {
        return _model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = SettingsViewBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        mBinding.model = model
        mBinding.fragment = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding.model = null
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
    fun onSelectApplicationsButtonClicked(v: View?) {
        findNavController().navigate(ActionOnlyNavDirections(R.id.action_settingsFragment_to_applicationsSelectionFragment))
    }


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
    }

    @dagger.Module
    class ModuleExt {
    }
}