package com.jakewharton.u2020.ui.logs;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.util.ShareUtils;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.u2020.data.LumberYard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public final class LogsDialog extends AlertDialog {
    private final LumberYard lumberYard;
    private final LogAdapter adapter;
    private EditText query;

    private CompositeSubscription subscriptions;

    public LogsDialog(Context context, LumberYard lumberYard) {
        super(context);
        this.lumberYard = lumberYard;

        adapter = new LogAdapter(context);

        View view = LayoutInflater.from(context).inflate(R.layout.debug_logs, null);
        ListView listView = view.findViewById(R.id.list);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setAdapter(adapter);

        query = view.findViewById(R.id.query);

        setTitle("Logs");
        setView(view);
        setButton(BUTTON_NEGATIVE, "Close", (dialog, which) -> {
            // NO-OP.
        });
        setButton(BUTTON_POSITIVE, "Share", (dialog, which) -> {
            share();
        });
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override protected void onStart() {
        super.onStart();

        adapter.setLogs(filterData(query.getText(), lumberYard.bufferedLogs(), false));

        subscriptions = new CompositeSubscription();
        subscriptions.add(lumberYard.logs() //
                .buffer(2, TimeUnit.SECONDS, Schedulers.computation())
                .map(data -> filterData(query.getText(), data, true))
                .filter(data -> !data.isEmpty())
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::addLogs));
        subscriptions.add(RxTextView.textChanges(query) //
                .debounce(1, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .observeOn(Schedulers.computation(), 1)
                .map(query -> filterData(query, lumberYard.bufferedLogs(), false))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::setLogs));
    }

    private List<LumberYard.Entry> filterData(CharSequence query, List<LumberYard.Entry> entries, boolean silent) {
        if (TextUtils.isEmpty(query)) {
            return entries;
        }
        if (!silent) {
            Timber.d("filterData() called with: query = %s;",
                    query);
        }
        long start = SystemClock.elapsedRealtime();
        List<LumberYard.Entry> result = new ArrayList<>();

        Pattern pattern = null;
        try {
            pattern = Pattern.compile(query.toString(), Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            Timber.e("Invalid pattern: %s", query);
        }
        if (pattern != null) {
            for (LumberYard.Entry entry : entries) {
                for (String value : new String[]{
                        entry.displayLevel(),
                        entry.displayTime(),
                        entry.tag,
                        entry.message
                }) {
                    if (pattern.matcher(value).find()) {
                        result.add(entry);
                        break;
                    }
                }
            }
        }
        if (!silent) {
            Timber.d("filterData: duration %d", SystemClock.elapsedRealtime() - start);
        }
        return result;
    }

    @Override protected void onStop() {
        super.onStop();
        subscriptions.clear();
    }

    private void share() {
        lumberYard.save() //
                .subscribeOn(Schedulers.io()) //
                .observeOn(AndroidSchedulers.mainThread()) //
                .subscribe(new Subscriber<File>() {
                    @Override public void onCompleted() {
                        // NO-OP.
                    }

                    @Override public void onError(Throwable e) {
                        Timber.e(e, null);
                        Toast.makeText(getContext(), "Couldn't save the logs for sharing.", Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override public void onNext(File file) {
                        ShareUtils.shareFile(ShareUtils.getAppFileProviderUri(file, getContext()), getContext());
                    }
                });
    }
}
