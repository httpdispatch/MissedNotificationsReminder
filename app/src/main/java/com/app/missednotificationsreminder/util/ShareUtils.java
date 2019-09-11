package com.app.missednotificationsreminder.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;

import androidx.core.content.FileProvider;
import timber.log.Timber;

/**
 * Various share functionality related utilities
 */
public class ShareUtils {
    /**
     * The file provider authority
     */
    static String getAuthority(Context context) {
        return context.getPackageName() + ".FILEPROVIDER";
    }

    /**
     * Share the file using android system dialog
     *
     * @param fileUri the file uri to share
     * @param context
     * @return true if share file intent has been handled, false otherwise
     */
    public static boolean shareFile(Uri fileUri, Context context) {
        return shareFile(fileUri, null, null, context);
    }

    /**
     * Share the file using android system dialog
     *
     * @param fileUri the file URI to share
     * @param text    the optional text extra for the sharing intent. This may be ignored by target applications, but
     *                applications such as mail client may use it
     * @param email   the optional email extra for the sharing intent. This may be ignored by target applications, but
     *                applications such as mail client may use it
     * @param context
     * @return true if share file intent has been handled, false otherwise
     */
    public static boolean shareFile(Uri fileUri, String text, String email, Context context) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        if (!TextUtils.isEmpty(text)) {
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        }
        if (!TextUtils.isEmpty(email)) {
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        }
        sendIntent.setType(getMimeType(fileUri.toString()));
        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        try {
            context.startActivity(sendIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            Timber.e(e, null);
            return false;
        }
    }

    /**
     * Get the mime type for the specified path
     *
     * @param url the file path (either local or web url)
     * @return mime type associated with the file extension if found, application/octet-stream otherwise
     */
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (type == null) {
            type = "application/octet-stream";
        }
        return type;
    }

    /**
     * Get the app file provider URI
     *
     * @param file    the fil to get the URI for
     * @param context
     * @return
     */
    public static Uri getAppFileProviderUri(File file, Context context) {
        Uri uri = FileProvider.getUriForFile(context, getAuthority(context), file);
        Timber.d("getAppFileProviderUri(): file = %s; uri = %s",
                file, uri);
        return uri;
    }

}
