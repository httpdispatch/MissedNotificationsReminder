package com.app.missednotificationsreminder.settings.applicationselection

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.settings.applicationselection.data.model.ApplicationItem
import com.app.missednotificationsreminder.databinding.ApplicationsSelectionViewBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import com.app.missednotificationsreminder.ui.widget.misc.DividerItemDecoration
import com.f2prateek.rx.preferences.Preference
import dagger.Binds
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays applications selection view
 *
 * @author Eugene Popovich
 */
class ApplicationsSelectionFragment : CommonFragmentWithViewBinding<ApplicationsSelectionViewBinding>(
        R.layout.applications_selection_view) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<ApplicationsSelectionViewModel> { viewModelFactory }

    @Inject
    lateinit var adapter: ApplicationsSelectionAdapter

    @ExperimentalCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // load the model data
        viewModel.loadData()
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    @ExperimentalCoroutinesApi
    private fun init() {
        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner

        viewDataBinding.list.layoutManager = LinearLayoutManager(context)
        viewDataBinding.list.adapter = adapter
        viewDataBinding.list.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL,
                        resources.getDimension(R.dimen.applications_divider_padding_start),
                        safeIsRtl()))
        viewModel.viewState
                .onEach { renderViewState(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun renderViewState(viewState: ViewState) {
        Timber.d("renderViewState: viewState=%s", viewState)
        when (viewState.loadingStatus) {
            is LoadingStatus.Loading -> viewDataBinding.animator.setDisplayedChild(viewDataBinding.loading)
            is LoadingStatus.Error -> viewDataBinding.animator.setDisplayedChild(viewDataBinding.error)
            is LoadingStatus.NotStarted -> {
                viewDataBinding.animator.setDisplayedChild(
                        if (viewState.data.isEmpty()) viewDataBinding.empty
                        else viewDataBinding.list)
                adapter.setData(viewState.data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.shutdown()
    }

    @dagger.Module
    abstract class Module {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ModuleExt::class])
        abstract fun contribute(): ApplicationsSelectionFragment

        @Binds
        @IntoMap
        @ViewModelKey(ApplicationsSelectionViewModel::class)
        internal abstract fun bindViewModel(viewmodel: ApplicationsSelectionViewModel): ViewModel
    }

    @dagger.Module
    class ModuleExt {

        @Provides
        @FragmentScope
        fun provideApplicationsCheckedStateChangeListener(
                @SelectedApplications selectedApplications: Preference<Set<String>>): ApplicationItemViewModel.ApplicationCheckedStateChangedListener {
            return ApplicationItemViewModel.ApplicationCheckedStateChangedListener { applicationItem: ApplicationItem, checked: Boolean ->
                Timber.d("Update selected application value %1\$s to %2\$b", applicationItem.packageName, checked)
                // for sure we may use if condition here instead of concatenation of 2 observables. Just wanted to achieve
                // same result with RxJava usage.
                (selectedApplications.get() ?: emptySet())
                        .let {
                            val updatedSet = it.toMutableSet()
                            if (updatedSet.contains(applicationItem.packageName))
                                updatedSet.remove(applicationItem.packageName)
                            else
                                updatedSet.add(applicationItem.packageName)
                            selectedApplications.set(updatedSet.toSet())
                        }
            }
        }
    }
}