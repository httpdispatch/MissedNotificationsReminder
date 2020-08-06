package com.app.missednotificationsreminder.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.missednotificationsreminder.di.Injector.Companion.obtain
import com.app.missednotificationsreminder.service.event.RemindEvents
import com.app.missednotificationsreminder.util.event.FlowEventBus
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

/**
 * The remind job
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RemindJob(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {
    init {
        Timber.d("RemindJob() called with: params = %s",
                params)
        // inject dependencies
        val appGraph: AndroidInjector<Any>? = obtain(context.applicationContext)
        appGraph!!.inject(this)
    }

    @Inject
    lateinit var mEventBus: FlowEventBus

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("doWork() called")
        mEventBus.toFlow()
                .filter { event -> event === RemindEvents.REMINDER_COMPLETED }
                .onStart {  mEventBus.send(RemindEvents.REMIND)  }
                .first()
        Timber.d("doWork() done")
        Result.success()
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): RemindJob
    }

    companion object {
        const val TAG = "REMIND_JOB"
    }
}