package com.app.missednotificationsreminder.service.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

/**
 * The utility class to parse notification related information
 */
public class NotificationParser {
    /*
     * Data constants used to parse notification view ids
     */
    public static final String NOTIFICATION_TITLE_DATA = "1";
    public static final String BIG_NOTIFICATION_TITLE_DATA = "8";
    public static final String INBOX_NOTIFICATION_TITLE_DATA = "9";
    /**
     * The id of the notification title view. Initialized in the {@link #detectNotificationIds()} method
     */
    public int mNotificationTitleId = 0;
    /**
     * The id of the big notification title view. Initialized in the {@link #detectNotificationIds()} method
     */
    public int mBigNotificationTitleId = 0;
    /**
     * The id of the inbox notification title view. Initialized in the {@link #detectNotificationIds()} method
     */
    public int mInboxNotificationTitleId = 0;
    /**
     * The application context
     */
    Context mContext;

    /**
     * Construct notification parser
     *
     * @param context
     */
    public NotificationParser(Context context) {
        mContext = context;
        detectNotificationIds();
    }

    /**
     * Detect required view ids which are used to parse notification information
     */
    private void detectNotificationIds() {
        Timber.d("detectNotificationIds");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                .setContentTitle(NOTIFICATION_TITLE_DATA);

        Notification n = mBuilder.build();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView;

        if(n.contentView != null) {
            // detect id's from normal view
            localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
            n.contentView.reapply(mContext, localView);
            recursiveDetectNotificationsIds(localView);
        }

        // detect id's from expanded views
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
            mBuilder.setContentTitle(BIG_NOTIFICATION_TITLE_DATA);
            mBuilder.setStyle(bigtextstyle);
            n = mBuilder.build();
            detectExpandedNotificationsIds(n);

            NotificationCompat.InboxStyle inboxStyle =
                    new NotificationCompat.InboxStyle();
            mBuilder.setContentTitle(INBOX_NOTIFICATION_TITLE_DATA);

            mBuilder.setStyle(inboxStyle);
            n = mBuilder.build();
            detectExpandedNotificationsIds(n);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void detectExpandedNotificationsIds(Notification n) {
        if(n.bigContentView != null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup localView = (ViewGroup) inflater.inflate(n.bigContentView.getLayoutId(), null);
            n.bigContentView.reapply(mContext, localView);
            recursiveDetectNotificationsIds(localView);
        }
    }

    private void recursiveDetectNotificationsIds(ViewGroup v) {
        for (int i = 0; i < v.getChildCount(); i++) {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup)
                recursiveDetectNotificationsIds((ViewGroup) child);
            else if (child instanceof TextView) {
                String text = ((TextView) child).getText().toString();
                int id = child.getId();
                switch (text) {
                    case NOTIFICATION_TITLE_DATA:
                        mNotificationTitleId = id;
                        break;
                    case BIG_NOTIFICATION_TITLE_DATA:
                        mBigNotificationTitleId = id;
                        break;
                    case INBOX_NOTIFICATION_TITLE_DATA:
                        mInboxNotificationTitleId = id;
                        break;
                }
            }
        }
    }

    // use reflection to extract string from remoteviews object
    private HashMap<Integer, CharSequence> getNotificationStringFromRemoteViews(RemoteViews view) {
        HashMap<Integer, CharSequence> notificationText = new HashMap<>();

        try {
            ArrayList<Parcelable> actions = null;
            Field fs = RemoteViews.class.getDeclaredField("mActions");
            if (fs != null) {
                fs.setAccessible(true);
                //noinspection unchecked
                actions = (ArrayList<Parcelable>) fs.get(view);
            }
            if (actions != null) {
                // Find the setText() and setTime() reflection actions
                for (Parcelable p : actions) {
                    Parcel parcel = Parcel.obtain();
                    p.writeToParcel(parcel, 0);
                    parcel.setDataPosition(0);

                    // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                    int tag = parcel.readInt();
                    if (tag != 2) continue;

                    // View ID
                    int viewId = parcel.readInt();

                    String methodName = parcel.readString();
                    //noinspection ConstantConditions
                    if (methodName == null) continue;

                        // Save strings
                    else if (methodName.equals("setText")) {
                        // Parameter type (10 = Character Sequence)
                        int i = parcel.readInt();

                        // Store the actual string
                        try {
                            CharSequence t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                            notificationText.put(viewId, t);
                        } catch (Exception exp) {
                            Timber.d("getNotificationStringFromRemoteViews: Can't get the text for setText with viewid:" + viewId + " parameter type:" + i + " reason:" + exp.getMessage());
                        }
                    }

                    parcel.recycle();
                }
            }
        } catch (Exception exp) {
            Timber.e(exp, null);
        }

        return notificationText;
    }

    private CharSequence extractTitleFromView(RemoteViews view) {
        CharSequence title = null;

        HashMap<Integer, CharSequence> notificationStrings = getNotificationStringFromRemoteViews(view);

        if (notificationStrings.size() > 0) {

            // get title string if available
            if (notificationStrings.containsKey(mNotificationTitleId)) {
                title = notificationStrings.get(mNotificationTitleId);
            } else if (notificationStrings.containsKey(mBigNotificationTitleId)) {
                title = notificationStrings.get(mBigNotificationTitleId);
            } else if (notificationStrings.containsKey(mInboxNotificationTitleId)) {
                title = notificationStrings.get(mInboxNotificationTitleId);
            }
        }

        return title;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews getBigContentView(Notification n) {
        if (n.bigContentView == null)
            return n.contentView;
        else {
            return n.bigContentView;
        }
    }

    private CharSequence getExpandedTitle(Notification n) {
        CharSequence title = null;

        RemoteViews view = n.contentView;

        // first get information from the original content view
        title = extractTitleFromView(view);

        // then try get information from the expanded view
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view = getBigContentView(n);
            title = extractTitleFromView(view);
        }
        Timber.d("getExpandedTitle: discovered title %1$s", title);
        return title;
    }

    /**
     * Get the title for the specified notification and package name
     *
     * @param notification
     * @param packageName
     * @return notification title if found. Otherwise returns package name.
     */
    public CharSequence getNotificationTitle(Notification notification, String packageName) {
        CharSequence title = null;
        title = getExpandedTitle(notification);
        if (title == null) {
            Bundle extras = NotificationCompat.getExtras(notification);
            if (extras != null) {
                Timber.d("getNotificationTitle: has extras: %1$s", extras.toString());
                title = extras.getCharSequence("android.title");
                Timber.d("getNotificationTitle: notification has no title, trying to get from bundle. found: %1$s", title);
            }
        }
        if (title == null) {
            // if title was not found, use package name as title
            title = packageName;
        }
        Timber.d("getNotificationTitle: discovered title %1$s", title);
        return title;
    }
}
