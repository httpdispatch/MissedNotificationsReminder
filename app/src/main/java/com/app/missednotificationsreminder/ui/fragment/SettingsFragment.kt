package com.app.missednotificationsreminder.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.SettingsViewModel
import com.app.missednotificationsreminder.databinding.SettingsViewBinding
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel
import com.app.missednotificationsreminder.ui.view.*
import com.jakewharton.u2020.data.LumberYard
import com.jakewharton.u2020.ui.logs.LogsDialog
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

/**
 * Fragment which displays other settings view
 *
 * @author Eugene Popovich
 */
class SettingsFragment : CommonFragmentWithViewModel<SettingsViewModel?>(), SettingsView {
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
        init(view, savedInstanceState)
    }

    private fun init(view: View, savedInstanceState: Bundle?) {
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
        model.checkServiceEnabled()
        model.checkBatteryOptimizationDisabled()
        model.checkPermissions()
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
        @Provides
        fun provideReminderView(fragment: SettingsFragment): ReminderView {
            return fragment.childFragmentManager
                    .findFragmentById(R.id.reminder_fragment) as ReminderFragment
        }

        @Provides
        fun provideSchedulerView(fragment: SettingsFragment): SchedulerView {
            return fragment.childFragmentManager
                    .findFragmentById(R.id.scheduler_fragment) as SchedulerFragment
        }

        @Provides
        fun provideSoundView(fragment: SettingsFragment): SoundView {
            return fragment.childFragmentManager
                    .findFragmentById(R.id.sound_fragment) as SoundFragment
        }

        @Provides
        fun provideVibrationView(fragment: SettingsFragment): VibrationView {
            return fragment.childFragmentManager
                    .findFragmentById(R.id.vibration_fragment) as VibrationFragment
        }

        @Provides
        fun provideSettingsView(fragment: SettingsFragment): SettingsView {
            return fragment
        }
    }
}