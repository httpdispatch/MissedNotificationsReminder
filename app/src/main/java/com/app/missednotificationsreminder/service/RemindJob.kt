package com.app.missednotificationsreminder.service

import com.app.missednotificationsreminder.di.Injector.Companion.obtain
import com.app.missednotificationsreminder.service.event.RemindEvents
import com.app.missednotificationsreminder.util.event.FlowEventBus
import com.evernote.android.job.Job
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * The remind job
 */
@OptIn(FlowPreview::class)
class RemindJob : Job() {
    @Inject
    lateinit var mEventBus: FlowEventBus

    override fun onRunJob(params: Params): Result {
        Timber.d("onRunJob() called with: params = %s",
                params)
        // inject dependencies
        val appGraph: AndroidInjector<Any>? = obtain(context.applicationContext)
        appGraph!!.inject(this)

        // request reminder and await for the reminder completed event
        runBlocking {
            mEventBus.toFlow()
                    .filter { event -> event === RemindEvents.REMINDER_COMPLETED }
                    .flatMapMerge { flow<Any> { mEventBus.send(RemindEvents.REMIND) } }
                    .collect { }
        }
        Timber.d("onRunJob() done")
        return Result.SUCCESS
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