package com.app.missednotificationsreminder.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.binding.model.IntervalViewModel;
import com.app.missednotificationsreminder.databinding.IntervalViewBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;
import com.app.missednotificationsreminder.ui.view.IntervalView;

import javax.inject.Inject;

/**
 * Fragment which displays interval settings view
 *
 * @author Eugene Popovich
 */
public class IntervalFragment extends CommonFragmentWithViewModel<IntervalViewModel> implements IntervalView {

    @Inject IntervalViewModel model;
    IntervalViewBinding mBinding;

    @Override public IntervalViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = IntervalViewBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view, savedInstanceState);
    }

    private void init(View view, Bundle savedInstanceState) {
        mBinding.setModel(model);
    }
}
