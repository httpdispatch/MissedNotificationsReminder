package com.app.missednotificationsreminder.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel;
import com.app.missednotificationsreminder.data.model.ApplicationItem;
import com.app.missednotificationsreminder.databinding.ApplicationsSelectionViewBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;
import com.app.missednotificationsreminder.ui.view.ApplicationsSelectionView;
import com.app.missednotificationsreminder.ui.widget.ApplicationsSelectionAdapter;
import com.app.missednotificationsreminder.ui.widget.misc.DividerItemDecoration;

import java.util.List;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Fragment which displays applications selection view
 *
 * @author Eugene Popovich
 */
public class ApplicationsSelectionFragment extends CommonFragmentWithViewModel<ApplicationsSelectionViewModel> implements ApplicationsSelectionView {

    // sequence is important: adapter should be before model, such as model refers to the
    // getListLoadedAction method during initialization
    @Inject ApplicationsSelectionAdapter adapter;
    @Inject ApplicationsSelectionViewModel model;
    ApplicationsSelectionViewBinding mBinding;

    @Override public ApplicationsSelectionViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = ApplicationsSelectionViewBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view, savedInstanceState);
    }

    private void init(View view, Bundle savedInstanceState) {
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mBinding.animator.setDisplayedChild(adapter.getItemCount() == 0 //
                        ? mBinding.empty //
                        : mBinding.list);
            }
        });

        mBinding.list.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.list.setAdapter(adapter);
        mBinding.list.addItemDecoration(
                new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL,
                        getResources().getDimension(R.dimen.applications_divider_padding_start),
                        safeIsRtl()));

        // load the model data
        model.loadData();
    }

    @Override public void setErrorState() {
        mBinding.animator.setDisplayedChild(mBinding.error);
    }

    @Override public Action1<List<ApplicationItem>> getListLoadedAction() {
        return adapter;
    }

    @Override public void setLoadingState() {
        if (mBinding.animator.getDisplayedChildId() != mBinding.list.getId()) {
            mBinding.animator.setDisplayedChild(mBinding.loading);
        }
    }
}
