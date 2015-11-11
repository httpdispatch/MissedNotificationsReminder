package com.app.missednotificationsreminder;


final class Modules {
    static Object[] list(CustomApplication app) {
        return new Object[]{
                new ApplicationModule(app),
                new DebugApplicationModule(),
        };
    }

    private Modules() {
        // No instances.
    }
}
