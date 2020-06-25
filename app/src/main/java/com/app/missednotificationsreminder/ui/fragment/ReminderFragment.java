package com.app.missednotificationsreminder.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.binding.model.ReminderViewModel;
import com.app.missednotificationsreminder.binding.model.SettingsViewModel;
import com.app.missednotificationsreminder.databinding.ReminderViewBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;

import javax.inject.Inject;

import dagger.android.ContributesAndroidInjector;

/**
 * Fragment which displays interval settings view
 *
 * @author Eugene Popovich
 */
public class ReminderFragment extends CommonFragmentWithViewModel<ReminderViewModel> {

    @Inject
    ReminderViewModel model;
    @Inject SettingsViewModel parentModel;
    ReminderViewBinding mBinding;

    @Override public ReminderViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = ReminderViewBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view, savedInstanceState);
    }

    private void init(View view, Bundle savedInstanceState) {
        mBinding.setModel(model);
        mBinding.setParentModel(parentModel);
    }

    @dagger.Module
    public static abstract class Module {
        @ContributesAndroidInjector
        abstract ReminderFragment contribute();
    }
}
