package com.app.missednotificationsreminder.data.model

import androidx.appcompat.app.AppCompatDelegate

enum class NightMode(val nightModeId: Int) {
    FOLLOW_SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    YES(AppCompatDelegate.MODE_NIGHT_YES),
    NO(AppCompatDelegate.MODE_NIGHT_NO)
}