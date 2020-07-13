package com.app.missednotificationsreminder.settings.vibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.databinding.FragmentVibrationBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;

import javax.inject.Inject;

import dagger.android.ContributesAndroidInjector;

/**
 * Fragment which displays vibration settings view
 *
 * @author Eugene Popovich
 */
public class VibrationFragment extends CommonFragmentWithViewModel<VibrationViewModel> {
    @Inject VibrationViewModel model;
    FragmentVibrationBinding mBinding;

    @Override public VibrationViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentVibrationBinding.inflate(inflater, container, false);
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

    @dagger.Module
    public static abstract class Module {
        @ContributesAndroidInjector
        abstract VibrationFragment contribute();
    }
}
