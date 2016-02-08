package com.app.missednotificationsreminder.ui.fragment.common;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.ui.activity.common.CommonFragmentActivity;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Inject;

import dagger.ObjectGraph;
import timber.log.Timber;

/**
 * Common fragment used as parent for all fragments in the application
 *
 * @author Eugene Popovich
 */
public class CommonFragment extends Fragment implements
        ActivityStateAccessor {
    @Inject RefWatcher mRefWatcher;


    void trackLifecycleEvent(String event) {
        Timber.d(event + ": " + getClass().getSimpleName());
    }

    public CommonFragment() {
        trackLifecycleEvent("Constructor");
    }

    @Override public void onAttach(Context contxt) {
        super.onAttach(contxt);
        trackLifecycleEvent("onAttach");
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackLifecycleEvent("onCreate");

        // Obtain the activity related object graph and inject dependencies
        ObjectGraph injector = Injector.obtain(getActivity());
        injector.inject(this);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        trackLifecycleEvent("onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override public void onDetach() {
        super.onDetach();
        trackLifecycleEvent("onDetach");
    }

    @Override public void onDestroy() {
        super.onDestroy();
        trackLifecycleEvent("onDestroy");
        mRefWatcher.watch(this);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        trackLifecycleEvent("onActivityCreated");
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        trackLifecycleEvent("onDestroyView");
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        trackLifecycleEvent("onSaveInstanceState");
    }

    @Override public void onResume() {
        super.onResume();
        trackLifecycleEvent("onResume");
    }

    @Override public void onPause() {
        super.onPause();
        trackLifecycleEvent("onPause");
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        trackLifecycleEvent("onViewCreated");
    }

    @Override public void onStart() {
        super.onStart();
        trackLifecycleEvent("onStart");
    }

    @Override public void onStop() {
        super.onStop();
        trackLifecycleEvent("onStop");
    }

    @Override public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        trackLifecycleEvent("onActivityResult");
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        trackLifecycleEvent("onCreateOptionsMenu");
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        trackLifecycleEvent("onPrepareOptionsMenu");
    }

    @Override public boolean isActivityAlive() {
        return getActivity() != null
                && ((ActivityStateAccessor) getActivity()).isActivityAlive();
    }

    @Override public boolean isActivityResumed() {
        return getActivity() != null && ((ActivityStateAccessor) getActivity()).isActivityResumed();
    }


    protected boolean safeIsRtl() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                ((CommonFragmentActivity) getActivity()).isRtl();
    }
}

