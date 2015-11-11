package com.app.missednotificationsreminder.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.SettingsActivityBinding;
import com.app.missednotificationsreminder.ui.activity.common.CommonFragmentActivity;
import com.app.missednotificationsreminder.ui.fragment.IntervalFragment;
import com.app.missednotificationsreminder.ui.fragment.SettingsFragment;

import dagger.ObjectGraph;

/**
 * Created by Eugene on 21.10.2015.
 */
public class SettingsActivity extends CommonFragmentActivity {
    /**
     * The view data mBinding
     */
    private SettingsActivityBinding mBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        mBinding = SettingsActivityBinding.inflate(inflater, getRootContainer(), true);
    }

    @Override
    protected ObjectGraph initializeActivityGraph(ObjectGraph appGraph) {
        return super.initializeActivityGraph(appGraph).plus(new SettingsActivityModule(this));
    }

    /**
     * Get the calling intent which launches {@link SettingsActivity}
     *
     * @param context the context which launches the activity
     * @return
     */
    public static Intent getCallingIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    /**
     * Get the {@link SettingsFragment} attached to the activity
     *
     * @return
     */
    public SettingsFragment getSettingsFragment() {
        return (SettingsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.settings_fragment);
    }

    /**
     * Get the {@link IntervalFragment} attached to the activity
     *
     * @return
     */
    public IntervalFragment getIntervalFragment() {
        return (IntervalFragment)
                getSettingsFragment().
                        getChildFragmentManager()
                        .findFragmentById(R.id.interval_fragment);
    }
}

