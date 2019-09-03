package com.app.missednotificationsreminder.data.model;

import android.net.Uri;

import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel;

/**
 * The class to store application item information used in the {@link ApplicationsSelectionViewModel}
 */
public class ApplicationItem {
    /**
     * Whether the application is already checked by user
     */
    public final boolean checked;
    /**
     * The application name
     */
    public final CharSequence applicationName;
    /**
     * The application package name
     */
    public final String packageName;
    /**
     * The application icon URI
     */
    public final Uri iconUri;
    /**
     * The number of active notifications
     */
    public final int activeNotifications;

    private ApplicationItem(Builder builder) {
        checked = builder.checked;
        applicationName = builder.applicationName;
        packageName = builder.packageName;
        iconUri = builder.iconUri;
        activeNotifications = builder.activeNotifications;
    }

    @Override public String toString() {
        return new StringBuilder()
                .append("ApplicationItem{")
                .append("checked=").append(checked)
                .append(", applicationName=").append(applicationName)
                .append(", packageName='").append(packageName).append('\'')
                .append(", iconUri=").append(iconUri)
                .append(", activeNotifications=").append(activeNotifications)
                .append("}")
                .toString();
    }

    public static final class Builder {
        private boolean checked;
        private CharSequence applicationName;
        private String packageName;
        private Uri iconUri;
        private int activeNotifications;

        public Builder() {
        }

        public Builder(ApplicationItem copy) {
            this.checked = copy.checked;
            this.applicationName = copy.applicationName;
            this.packageName = copy.packageName;
            this.iconUri = copy.iconUri;
            this.activeNotifications = copy.activeNotifications;
        }

        public Builder checked(boolean checked) {
            this.checked = checked;
            return this;
        }

        public Builder applicationName(CharSequence applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder iconUri(Uri iconUri) {
            this.iconUri = iconUri;
            return this;
        }

        public Builder activeNotifications(int activeNotifications) {
            this.activeNotifications = activeNotifications;
            return this;
        }

        public ApplicationItem build() {
            return new ApplicationItem(this);
        }
    }
}
