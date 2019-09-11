package com.jakewharton.u2020.ui.logs;

import android.content.Context;
import android.widget.ListView;
import android.widget.Toast;

import com.app.missednotificationsreminder.util.ShareUtils;
import com.jakewharton.u2020.data.LumberYard;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public final class LogsDialog extends AlertDialog {
  private final LumberYard lumberYard;
  private final LogAdapter adapter;

  private CompositeSubscription subscriptions;

  public LogsDialog(Context context, LumberYard lumberYard) {
    super(context);
    this.lumberYard = lumberYard;

    adapter = new LogAdapter(context);

    ListView listView = new ListView(context);
    listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
    listView.setAdapter(adapter);

    setTitle("Logs");
    setView(listView);
    setButton(BUTTON_NEGATIVE, "Close", (dialog, which) -> {
      // NO-OP.
    });
    setButton(BUTTON_POSITIVE, "Share", (dialog, which) -> {
      share();
    });
  }

  @Override protected void onStart() {
    super.onStart();

    adapter.setLogs(lumberYard.bufferedLogs());

    subscriptions = new CompositeSubscription();
    subscriptions.add(lumberYard.logs() //
        .observeOn(AndroidSchedulers.mainThread()) //
        .subscribe(adapter));
  }

  @Override protected void onStop() {
    super.onStop();
    subscriptions.unsubscribe();
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
