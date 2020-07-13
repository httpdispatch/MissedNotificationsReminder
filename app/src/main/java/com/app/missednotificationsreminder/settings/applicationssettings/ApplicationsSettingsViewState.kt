package com.app.missednotificationsreminder.settings.applicationssettings

data class ApplicationsSettingsViewState(
        val ignorePersistentNotifications: Boolean,
        val respectPhoneCalls: Boolean,
        val respectRingerMode: Boolean,
        val remindWhenScreenIsOn: Boolean)