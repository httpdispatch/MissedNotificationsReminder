package com.app.missednotificationsreminder.util.moshi

import com.squareup.moshi.JsonAdapter
import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.Preference
import com.tfcporciuncula.flow.Serializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import timber.log.Timber

/**
 * The moshi preference wrapper to wrap JSON containing preferences
 *
 * @param prefs        the preferences
 * @param key          the key of the target preference
 * @param defaultValue the default preference value
 * @param adapter      the moshi json adapter
 * @param <T>
 **/
class MoshiPreferenceWrapper<T : Any>(
        prefs: FlowSharedPreferences,
        key: String,
        defaultValue: T,
        adapter: JsonAdapter<T>) : Preference<T> {

    /**
     * The original preference
     */
    val preference: Preference<T>

    /**
     *
     */
    init {
        preference = prefs.getObject(key, object : Serializer<T> {
            override fun deserialize(serialized: String): T {
                return try {
                    val value = adapter.fromJson(serialized)
                    if (value != null && validate(value)) {
                        value
                    } else {
                        Timber.d("Validation failed for %s", key)
                        delete()
                        get()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    delete()
                    get()
                }
            }

            override fun serialize(value: T): String {
                return adapter.toJson(value)
            }
        }, defaultValue)
        get()
    }

    fun validate(item: T): Boolean {
        return true
    }

    override fun asCollector(): FlowCollector<T> {
        return preference.asCollector()
    }

    override fun asFlow(): Flow<T> {
        return preference.asFlow()
    }

    override fun asSyncCollector(throwOnFailure: Boolean): FlowCollector<T> {
        return preference.asSyncCollector(throwOnFailure)
    }

    override fun delete() {
        preference.delete()
    }

    override suspend fun deleteAndCommit(): Boolean {
        return preference.deleteAndCommit()
    }

    override fun get(): T {
        return preference.get()
    }

    override fun isNotSet(): Boolean {
        return preference.isNotSet()
    }

    override fun isSet(): Boolean {
        return preference.isSet()
    }

    override fun set(value: T) {
        return preference.set(value)
    }

    override suspend fun setAndCommit(value: T): Boolean {
        return preference.setAndCommit(value)
    }

    override val defaultValue: T
        get() = preference.defaultValue

    override val key: String
        get() = preference.key
}