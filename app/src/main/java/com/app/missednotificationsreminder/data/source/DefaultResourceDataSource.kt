package com.app.missednotificationsreminder.data.source

import android.content.Context
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import javax.inject.Inject

class DefaultResourceDataSource @Inject constructor(
        @param:ForApplication private val applicationContext: Context
) : ResourceDataSource {
    override fun getString(id: Int, vararg args: Any): String {
        return applicationContext.getString(id, *args)
    }
}