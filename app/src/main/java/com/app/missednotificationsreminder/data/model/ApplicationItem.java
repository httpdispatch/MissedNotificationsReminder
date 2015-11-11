package com.app.missednotificationsreminder.data.model;

import android.content.pm.PackageInfo;

import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel;

/**
 * The class to store application item information used in the {@link ApplicationsSelectionViewModel}
 *
 * @author Eugene Popovich
 */
public class ApplicationItem {
    private boolean mChecked;
    private PackageInfo mPpackageInfo;

    /**
     * Creates the ApplicationItem object
     *
     * @param checked      whether the application is already checked by user
     * @param ppackageInfo the application package information
     */
    public ApplicationItem(boolean checked, PackageInfo ppackageInfo) {
        mChecked = checked;
        mPpackageInfo = ppackageInfo;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public PackageInfo getPpackageInfo() {
        return mPpackageInfo;
    }

    public void setPpackageInfo(PackageInfo ppackageInfo) {
        mPpackageInfo = ppackageInfo;
    }
}
