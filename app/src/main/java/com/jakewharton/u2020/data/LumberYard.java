package com.jakewharton.u2020.data;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.temporal.ChronoField;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import okio.BufferedSink;
import okio.Okio;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Singleton
public final class LumberYard {
    private static final int BUFFER_SIZE = 10000;

    private final Application app;

    private final Deque<Entry> entries = new ArrayDeque<>(BUFFER_SIZE + 1);
    private final PublishSubject<Entry> entrySubject = PublishSubject.create();

    @Inject public LumberYard(Application app) {
        this.app = app;
    }

    public Timber.Tree tree() {
        return new Timber.DebugTree() {
            @Override protected void log(int priority, String tag, String message, Throwable t) {
                addEntry(new Entry(LocalDateTime.now(), priority, tag, message));
            }
        };
    }

    private synchronized void addEntry(Entry entry) {
        entries.addLast(entry);
        if (entries.size() > BUFFER_SIZE) {
            entries.removeFirst();
        }

        entrySubject.onNext(entry);
    }

    public List<Entry> bufferedLogs() {
        return new ArrayList<>(entries);
    }

    public Observable<Entry> logs() {
        return entrySubject;
    }

    /**
     * Save the current logs to disk.
     */
    public Observable<File> save() {
        return Observable.create(new Observable.OnSubscribe<File>() {
            @Override public void call(Subscriber<? super File> subscriber) {
                File folder = app.getExternalFilesDir(null);
                if (folder == null) {
                    subscriber.onError(new IOException("External storage is not mounted."));
                    return;
                }

                String fileName = ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + ".txt";
                // replace ':' char to avoid file saving issue on some devices
                fileName = fileName.replaceAll("\\:", "_");
                File output = new File(folder, fileName);

                BufferedSink sink = null;
                try {
                    sink = Okio.buffer(Okio.sink(output));
                    List<Entry> entries = bufferedLogs();
                    for (Entry entry : entries) {
                        sink.writeUtf8(entry.prettyPrint()).writeByte('\n');
                    }

                    subscriber.onNext(output);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                } finally {
                    if (sink != null) {
                        try {
                            sink.close();
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Delete all of the log files saved to disk. Be careful not to call this before any intents have
     * finished using the file reference.
     */
    public void cleanUp() {
        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... folders) {
                File folder = app.getExternalFilesDir(null);
                if (folder != null) {
                    File[] files = folder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".log")) {
                                file.delete();
                            }
                        }
                    }
                }

                return null;
            }
        }.execute();
    }

    public static final class Entry {
        static DateTimeFormatter formatter = (new DateTimeFormatterBuilder())
                .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalStart()
                .appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
                .toFormatter();
        public final LocalDateTime time;
        public final int level;
        public final String tag;
        public final String message;

        public Entry(LocalDateTime time, int level, String tag, String message) {
            this.time = time;
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        public String prettyPrint() {
            return String.format("%s %22s %s %s", displayTime(), tag, displayLevel(),
                    // Indent newlines to match the original indentation.
                    message.replaceAll("\\n", "\n                         "));
        }

        public String displayLevel() {
            switch (level) {
                case Log.VERBOSE:
                    return "V";
                case Log.DEBUG:
                    return "D";
                case Log.INFO:
                    return "I";
                case Log.WARN:
                    return "W";
                case Log.ERROR:
                    return "E";
                case Log.ASSERT:
                    return "A";
                default:
                    return "?";
            }
        }

        public String displayTime() {
            return formatter.format(time);
        }
    }
}
