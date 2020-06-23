package com.app.missednotificationsreminder.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.ApplicationItemViewModel
import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel
import com.app.missednotificationsreminder.data.model.ApplicationItem
import com.app.missednotificationsreminder.databinding.ApplicationsSelectionViewBinding
import com.app.missednotificationsreminder.di.qualifiers.ActivityScope
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel
import com.app.missednotificationsreminder.ui.view.ApplicationsSelectionView
import com.app.missednotificationsreminder.ui.widget.ApplicationsSelectionAdapter
import com.app.missednotificationsreminder.ui.widget.misc.DividerItemDecoration
import com.f2prateek.rx.preferences.Preference
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import rx.functions.Action1
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays applications selection view
 *
 * @author Eugene Popovich
 */
class ApplicationsSelectionFragment : CommonFragmentWithViewModel<ApplicationsSelectionViewModel?>(), ApplicationsSelectionView {
    // sequence is important: adapter should be before model, such as model refers to the
    // getListLoadedAction method during initialization
    @Inject
    lateinit var adapter: ApplicationsSelectionAdapter

    @Inject
    lateinit var _model: ApplicationsSelectionViewModel
    lateinit var mBinding: ApplicationsSelectionViewBinding
    
    override fun getModel(): ApplicationsSelectionViewModel {
        return _model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = ApplicationsSelectionViewBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view, savedInstanceState)
    }

    private fun init(view: View, savedInstanceState: Bundle?) {
        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                mBinding.animator.setDisplayedChild(if (adapter.itemCount == 0 //
                ) mBinding.empty //
                else mBinding.list)
            }
        })
        mBinding.list.layoutManager = LinearLayoutManager(context)
        mBinding.list.adapter = adapter
        mBinding.list.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL,
                        resources.getDimension(R.dimen.applications_divider_padding_start),
                        safeIsRtl()))

        // load the model data
        _model.loadData()
    }

    override fun setErrorState() {
        mBinding.animator.setDisplayedChild(mBinding.error)
    }

    override fun getListLoadedAction(): Action1<List<ApplicationItem>> {
        return Action1 { data: List<ApplicationItem>? -> adapter.setData(data) }
    }

    override fun setLoadingState() {
        if (mBinding.animator.displayedChildId != mBinding.list.id) {
            mBinding.animator.setDisplayedChild(mBinding.loading)
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
    }

    @dagger.Module
    class ModuleExt {
        @Provides
        fun provideApplicationsSelectionView(fragment: ApplicationsSelectionFragment): ApplicationsSelectionView {
            return fragment
        }

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
                            val updatedSet = it.toMutableSet();
                            if (updatedSet.contains(applicationItem.packageName))
                                updatedSet.remove(applicationItem.packageName)
                            else
                                updatedSet.add(applicationItem.packageName)
                            selectedApplications.set(updatedSet.toSet());
                        }
            }
        }
    }
}