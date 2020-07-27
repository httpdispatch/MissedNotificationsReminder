package com.app.missednotificationsreminder.settings.scheduler

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.FragmentSchedulerBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import com.app.missednotificationsreminder.util.TimeUtils
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import dagger.Binds
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

/**
 * Fragment which displays scheduler settings view
 *
 */
@FlowPreview
@ExperimentalCoroutinesApi
class SchedulerFragment : CommonFragmentWithViewBinding<FragmentSchedulerBinding>(R.layout.fragment_scheduler) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by viewModels<SchedulerViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        viewDataBinding.apply {
            // Set the lifecycle owner to the lifecycle of the view
            lifecycleOwner = viewLifecycleOwner
            fragment = this@SchedulerFragment
            viewModel = this@SchedulerFragment.viewModel
            viewState = this@SchedulerFragment.viewModel.viewState.asLiveData()
        }
    }

    fun selectTime(currentValue: Int, minMinutes: Int, maxMinutes: Int, updater: (Int) -> Unit) {
        // launch the time picker dialog
        val tpg = TimePickerDialog.newInstance(
                { _, hourOfDay: Int, minute: Int, _ -> updater(hourOfDay * TimeUtils.MINUTES_IN_HOUR + minute) },
                currentValue / TimeUtils.MINUTES_IN_HOUR, currentValue % TimeUtils.MINUTES_IN_HOUR, true)
        tpg.setMinTime(timepointFromMinutes(minMinutes))
        tpg.setMaxTime(timepointFromMinutes(maxMinutes))
        tpg.dismissOnPause(true)
        tpg.show(requireActivity().supportFragmentManager, tpg.javaClass.simpleName)
    }

    /**
     * Method which is called when the begin input is clicked. It launches the time selection dialog
     *
     * @param v
     */
    fun onBeginClicked(v: View?) {
        selectTime(
                viewModel.viewState.value.begin,
                viewModel.viewState.value.minimum,
                viewModel.viewState.value.end) {
            viewModel.process(SchedulerViewStatePartialChanges.BeginChange(it))
        }
    }

    /**
     * Method which is called when the end input is clicked. It launches the time selection dialog
     *
     * @param v
     */
    fun onEndClicked(v: View?) {
        selectTime(
                viewModel.viewState.value.end,
                viewModel.viewState.value.begin,
                viewModel.viewState.value.maximum ) {
            viewModel.process(SchedulerViewStatePartialChanges.EndChange(it))
        }
    }

    /**
     * Get the TimePoint instance for the specified minutes of day value
     *
     * @param minutes the minutes of day
     * @return
     */
    fun timepointFromMinutes(minutes: Int): Timepoint {
        return Timepoint(minutes / TimeUtils.MINUTES_IN_HOUR, minutes % TimeUtils.MINUTES_IN_HOUR, 0)
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): SchedulerFragment

        @Binds
        @IntoMap
        @ViewModelKey(SchedulerViewModel::class)
        internal abstract fun bindViewModel(viewModel: SchedulerViewModel): ViewModel
    }
}