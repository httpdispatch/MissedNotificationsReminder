package com.app.missednotificationsreminder.settings.reminder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.FragmentReminderBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.settings.SettingsViewModel
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import dagger.Binds
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

/**
 * Fragment which displays interval settings view
 */
@FlowPreview
@ExperimentalCoroutinesApi
class ReminderFragment : CommonFragmentWithViewBinding<FragmentReminderBinding>(R.layout.fragment_reminder) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<ReminderViewModel> { viewModelFactory }

    @Inject
    lateinit var parentModel: SettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        viewDataBinding.apply {
            // Set the lifecycle owner to the lifecycle of the view
            lifecycleOwner = viewLifecycleOwner
            parentModel = this@ReminderFragment.parentModel
            viewModel = this@ReminderFragment.viewModel
            viewState = this@ReminderFragment.viewModel.viewState.asLiveData()
        }
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): ReminderFragment

        @Binds
        @IntoMap
        @ViewModelKey(ReminderViewModel::class)
        internal abstract fun bindViewModel(viewModel: ReminderViewModel): ViewModel
    }
}