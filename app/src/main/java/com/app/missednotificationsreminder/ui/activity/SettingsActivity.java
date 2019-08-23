package com.app.missednotificationsreminder.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.SettingsActivityBinding;
import com.app.missednotificationsreminder.ui.activity.common.CommonFragmentActivity;
import com.app.missednotificationsreminder.ui.fragment.ReminderFragment;
import com.app.missednotificationsreminder.ui.fragment.SchedulerFragment;
import com.app.missednotificationsreminder.ui.fragment.SettingsFragment;
import com.app.missednotificationsreminder.ui.fragment.SoundFragment;
import com.app.missednotificationsreminder.ui.fragment.VibrationFragment;

import dagger.ObjectGraph;

/**
 * Main settings activity
 *
 * @author Eugene Popovich
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

        // can't use binding here because of release bug (http://stackoverflow.com/a/30880378/527759)
		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
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
     * Get the {@link ReminderFragment} attached to the activity
     *
     * @return
     */
    public ReminderFragment getReminderFragment() {
        return (ReminderFragment)
                getSettingsFragment().
                        getChildFragmentManager()
                        .findFragmentById(R.id.reminder_fragment);
    }

    /**
     * Get the {@link SchedulerFragment} attached to the activity
     *
     * @return
     */
    public SchedulerFragment getSchedulerFragment() {
        return (SchedulerFragment)
                getSettingsFragment().
                        getChildFragmentManager()
                        .findFragmentById(R.id.scheduler_fragment);
    }
    
    /**
     * Get the {@link SoundFragment} attached to the activity
     *
     * @return
     */
    public SoundFragment getSoundFragment() {
        return (SoundFragment)
                getSettingsFragment().
                        getChildFragmentManager()
                        .findFragmentById(R.id.sound_fragment);
    }
    
    /**
     * Get the {@link VibrationFragment} attached to the activity
     *
     * @return
     */
    public VibrationFragment getVibrationFragment() {
        return (VibrationFragment)
                getSettingsFragment().
                        getChildFragmentManager()
                        .findFragmentById(R.id.vibration_fragment);
    }
}

