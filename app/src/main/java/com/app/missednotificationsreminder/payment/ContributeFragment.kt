package com.app.missednotificationsreminder.payment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.onSuccess
import com.app.missednotificationsreminder.databinding.FragmentContributeBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.payment.di.PurchaseDataModule
import com.app.missednotificationsreminder.settings.SettingsFragment
import com.app.missednotificationsreminder.settings.SettingsViewState
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import dagger.Binds
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays contribute information
 */
@ExperimentalCoroutinesApi
class ContributeFragment : CommonFragmentWithViewBinding<FragmentContributeBinding>(
        R.layout.fragment_contribute) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<PurchaseViewModel> { viewModelFactory }

    @Inject
    lateinit var adapter: PurchaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // load the model data
        viewModel.loadData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        with(viewDataBinding) {
            // Set the lifecycle owner to the lifecycle of the view
            lifecycleOwner = viewLifecycleOwner
            viewState = viewModel.viewState.asLiveData()

            val defaultSpanSize = resources.getInteger(R.integer.purchase_columns_count)
            list.layoutManager = GridLayoutManager(activity, defaultSpanSize)
                    .apply {
                        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return if (position == 0) defaultSpanSize else 1
                            }

                        }
                    }
            list.adapter = adapter
        }

        flowOf(viewModel.viewEffect,
                adapter.viewEffect)
                .flatMapMerge { it }
                .onEach { renderViewEffect(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun renderViewEffect(viewEffect: PurchaseViewEffect) {
        Timber.d("renderViewEffect() called with: viewEffect = $viewEffect")
        when (viewEffect) {
            is PurchaseViewEffect.Purchase -> viewModel.purchase(viewEffect.skuDetails, requireActivity())
            is PurchaseViewEffect.Message -> findNavController().navigate(ContributeFragmentDirections.actionContributionFragmentToAlertDialogFragment(
                    message = viewEffect.message))
        }
    }

    @dagger.Module(includes = [PurchaseDataModule::class])
    abstract class Module {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ModuleExt::class])
        abstract fun contribute(): ContributeFragment

        @Binds
        @IntoMap
        @ViewModelKey(PurchaseViewModel::class)
        internal abstract fun bindViewModel(viewmodel: PurchaseViewModel): ViewModel
    }

    @dagger.Module
    class ModuleExt {

        @Provides
        fun providePurchaseViewState(fragment: ContributeFragment): StateFlow<PurchaseViewState> {
            return fragment.viewModel.viewState
        }
    }
}