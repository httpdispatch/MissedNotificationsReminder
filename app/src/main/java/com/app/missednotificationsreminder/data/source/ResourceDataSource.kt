package com.app.missednotificationsreminder.data.source

interface ResourceDataSource {
    fun getString(id: Int): String
}