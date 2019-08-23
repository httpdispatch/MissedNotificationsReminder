package com.app.missednotificationsreminder.service;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;


public class ReminderServiceJobCreator implements JobCreator {

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case RemindJob.TAG:
                return new RemindJob();
            default:
                return null;
        }
    }
}