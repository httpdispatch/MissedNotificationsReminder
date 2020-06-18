package com.app.missednotificationsreminder.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.ApplicationsSelectionActivityBinding;
import com.app.missednotificationsreminder.ui.activity.common.CommonFragmentActivity;
import com.app.missednotificationsreminder.ui.fragment.ApplicationsSelectionFragment;

/**
 * Applications selection activity
 *
 * @author Eugene Popovich
 */
public class ApplicationsSelectionActivity extends CommonFragmentActivity {

    /**
     * The view data binding
     */
    private ApplicationsSelectionActivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
        mBinding = ApplicationsSelectionActivityBinding.inflate(inflater, getRootContainer(), true);

        // set toolbar as actionbar and enable back navigation icon
        setSupportActionBar(mBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mBinding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Get the calling intent which launches {@link ApplicationsSelectionActivity}
     *
     * @param context the context which launches the activity
     * @return
     */
    public static Intent getCallingIntent(Context context) {
        return new Intent(context, ApplicationsSelectionActivity.class);
    }

    /**
     * Get the {@link ApplicationsSelectionFragment} attached to the activity
     *
     * @return
     */
    public ApplicationsSelectionFragment getApplicationsSelectionFragment() {
        return (ApplicationsSelectionFragment) getSupportFragmentManager()
                .findFragmentById(R.id.applications_selection_fragment);
    }
}

