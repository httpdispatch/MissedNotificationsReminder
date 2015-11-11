package com.app.missednotificationsreminder.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.binding.model.SettingsViewModel;
import com.app.missednotificationsreminder.databinding.SettingsViewBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;
import com.app.missednotificationsreminder.ui.view.SettingsView;

import javax.inject.Inject;

/**
 * Fragment which displays other settings view
 *
 * @author Eugene Popovich
 */
public class SettingsFragment extends CommonFragmentWithViewModel<SettingsViewModel> implements SettingsView {

    @Inject SettingsViewModel model;
    SettingsViewBinding mBinding;

    @Override public SettingsViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = SettingsViewBinding.inflate(inflater, container, false);
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

    @Override public void onResume() {
        super.onResume();
        model.checkServiceEnabled();
    }
}

