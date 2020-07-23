package com.app.missednotificationsreminder.settings.vibration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.FragmentVibrationBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import dagger.Binds
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

/**
 * Fragment which displays vibration settings view
 */
@FlowPreview
@ExperimentalCoroutinesApi
class VibrationFragment : CommonFragmentWithViewBinding<FragmentVibrationBinding>(R.layout.fragment_vibration) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<VibrationViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        viewDataBinding.apply {
            // Set the lifecycle owner to the lifecycle of the view
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@VibrationFragment.viewModel
            viewState = this@VibrationFragment.viewModel.viewState.asLiveData()
        }
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): VibrationFragment

        @Binds
        @IntoMap
        @ViewModelKey(VibrationViewModel::class)
        internal abstract fun bindViewModel(viewModel: VibrationViewModel): ViewModel
    }
}