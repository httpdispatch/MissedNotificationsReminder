package com.app.missednotificationsreminder.data.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel;
import com.app.missednotificationsreminder.data.model.util.ApplicationIconHandler;

/**
 * The class to store application item information used in the {@link ApplicationsSelectionViewModel}
 *
 * @author Eugene Popovich
 */
public class ApplicationItem {
    private boolean mChecked;
    private CharSequence mApplicationName;
    private String mPackageName;
    private Uri mIconUri;

    /**
     * Creates the ApplicationItem object
     *
     * @param checked        whether the application is already checked by user
     * @param packageInfo   the application package information
     * @param packageManager the package manager instance
     */
    public ApplicationItem(boolean checked, PackageInfo packageInfo, PackageManager packageManager) {
        mChecked = checked;
        mApplicationName = packageInfo.applicationInfo.loadLabel(packageManager);
        mPackageName = packageInfo.packageName;
        mIconUri = new Uri.Builder()
                .scheme(ApplicationIconHandler.SCHEME)
                .authority(mPackageName)
                .build();
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public CharSequence getApplicationName() {
        return mApplicationName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean hasIcon() {
        return mIconUri != null;
    }

    public Uri getIconUri() {
        return mIconUri;
    }
}
