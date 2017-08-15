package com.app.missednotificationsreminder.binding.model;

import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.data.model.ApplicationItem;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import timber.log.Timber;

/**
 * The view model for the single application item
 *
 * @author Eugene Popovich
 */
public class ApplicationItemViewModel extends BaseViewModel {
    /**
     * Data binding field to store application checked state
     */
    public BindableBoolean checked = new BindableBoolean();

    ApplicationItem mApplicationItem;
    private ApplicationCheckedStateChangedListener mApplicationCheckedStateChangedListener;
    private Picasso mPicasso;

    /**
     * @param checked                                the current application checked state
     * @param packageInfo                            the application package info
     * @param packageManager
     * @param picasso
     * @param applicationCheckedStateChangedListener the listener to subscribe to the on checked
     *                                               state changed event
     */
    public ApplicationItemViewModel(
            ApplicationItem applicationItem,
            Picasso picasso,
            ApplicationCheckedStateChangedListener applicationCheckedStateChangedListener) {
        Timber.d("Constructor");
        mApplicationItem = applicationItem;
        mPicasso = picasso;
        mApplicationCheckedStateChangedListener = applicationCheckedStateChangedListener;
        this.checked.set(applicationItem.isChecked());
        if (mApplicationCheckedStateChangedListener != null) {
            monitor(RxBindingUtils
                    .valueChanged(this.checked)
                    .skip(1) // skip the current value processing, which is passed automatically
                    .subscribe(value -> {
                        Timber.d("Checked property changed for %1$s", toString());
                        mApplicationCheckedStateChangedListener.onApplicationCheckedStateChanged
                                (mApplicationItem, value);
                    }));
        }
    }

    /**
     * Get the application name
     *
     * @return
     */
    public CharSequence getName() {
        Timber.d("getName for %1$s", toString());
        return mApplicationItem.getApplicationName();
    }

    /**
     * Get the application description
     *
     * @return
     */
    public String getDescription() {
        Timber.d("getDescription for %1$s", toString());
        return mApplicationItem.getPackageName();
    }

    /**
     * Get the application icon request
     *
     * @return
     */
    public RequestCreator getIcon() {
        Timber.d("getIcon for %1$s", toString());
        return mApplicationItem.hasIcon() ? mPicasso.load(mApplicationItem.getIconUri())
                .fit() : null;
    }

    /**
     * Reverse checked state. Called when the application item clicked. Method binded directly in
     * the layout xml
     *
     * @param v
     */
    public void onItemClicked(View v) {
        Timber.d("onItemClicked for %1$s", toString());
        checked.set(!checked.get());
    }

    @Override
    public String toString() {
        return String.format("%1$s(checked=%2$b, package=%3$s)", getClass().getSimpleName(), checked.get(), mApplicationItem.getPackageName());
    }

    /**
     * The interface subscribers to the onApplicationCheckedStateChanged event should implement
     */
    public static interface ApplicationCheckedStateChangedListener {
        void onApplicationCheckedStateChanged(ApplicationItem applicationItem, boolean checked);
    }
}
