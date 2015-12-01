package com.app.missednotificationsreminder;

/**
 * Custom application implementation to provide additional functionality such as dependency injection, leak inspection
 *
 * @author Eugene Popovich
 */
public class CustomApplication extends CustomApplicationBase {

    @Override Object[] getModules() {
        return Modules.list(this);
    }
}
