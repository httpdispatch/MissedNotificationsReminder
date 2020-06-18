package com.app.missednotificationsreminder.service;

import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.service.event.RemindEvents;
import com.app.missednotificationsreminder.util.event.RxEventBus;
import com.evernote.android.job.Job;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.AndroidInjector;
import dagger.android.ContributesAndroidInjector;
import rx.Completable;
import timber.log.Timber;

/**
 * The remind job
 */

public class RemindJob extends Job {

    public static final String TAG = "REMIND_JOB";

    @Inject RxEventBus mEventBus;

    @NonNull @Override protected Result onRunJob(@NonNull Params params) {
        Timber.d("onRunJob() called with: params = %s",
                params);
        // inject dependencies
        AndroidInjector appGraph = Injector.obtain(getContext().getApplicationContext());
        appGraph.inject(this);

        // request reminder and await for the reminder completed event
        mEventBus.toObserverable()
                .filter(event -> event == RemindEvents.REMINDER_COMPLETED)
                .mergeWith(Completable.fromAction(() -> mEventBus.send(RemindEvents.REMIND)).toObservable())
                .toBlocking()
                .first();
        Timber.d("onRunJob() done");
        return Result.SUCCESS;
    }

    @dagger.Module
    public static abstract class Module {
        @ContributesAndroidInjector
        abstract RemindJob contribute();
    }
}
