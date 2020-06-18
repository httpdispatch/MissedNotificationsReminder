package com.app.missednotificationsreminder.ui.activity.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.ui.AppContainer;
import com.app.missednotificationsreminder.ui.fragment.common.ActivityStateAccessor;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import timber.log.Timber;

/**
 * Common activity used as parent for all activities in the application
 *
 * @author Eugene Popovich
 */
public class CommonFragmentActivity extends DaggerAppCompatActivity implements
        ActivityStateAccessor {

    @Inject
    AppContainer appContainer;

    /**
     * Whether activity is alive flag. Handled in onCreate, onDestroy methods
     */
    private boolean mActivityAlive;

    /**
     * Whether activity is resumed flag. Handled in onResume, onPause methods
     */
    private boolean mResumed = false;

    /**
     * The root view container
     */
    private ViewGroup mContainer;

    void trackLifecycleEvent(String event) {
        Timber.d(event + ": " + getClass().getSimpleName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackLifecycleEvent("onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackLifecycleEvent("onStop");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Explicitly reference the application object since we don't want to match our own injector.
        super.onCreate(savedInstanceState);
        trackLifecycleEvent("onCreate");

        // obtain the root view container
        mContainer = appContainer.bind(this);

        mActivityAlive = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackLifecycleEvent("onDestroy");
        mActivityAlive = false;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        trackLifecycleEvent("onSaveInstanceState");
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackLifecycleEvent("onResume");
        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        trackLifecycleEvent("onPause");
        mResumed = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        trackLifecycleEvent("onActivityResult");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        trackLifecycleEvent("onNewIntent");
    }

    @Override
    public void finish() {
        super.finish();
        trackLifecycleEvent("finish");
    }

    @Override
    public boolean isActivityAlive() {
        return mActivityAlive;
    }

    @Override
    public boolean isActivityResumed() {
        return mResumed;
    }

    /**
     * Obtain the instance of the CommonFragmentActivity from the activity context or the context wrapper if possible
     *
     * @param context
     * @return
     */
    public static CommonFragmentActivity getFromContext(Context context) {
        if (context == null)
            return null;
        else if (context instanceof CommonFragmentActivity)
            return (CommonFragmentActivity) context;
        else if (context instanceof ContextWrapper)
            return getFromContext(((ContextWrapper) context).getBaseContext());

        return null;
    }

    /**
     * Get the root view container
     *
     * @return
     */
    public ViewGroup getRootContainer() {
        return mContainer;
    }

    /**
     * Is the activity in RTL orientation
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) public boolean isRtl() {
        return getRootContainer().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
}
