package com.app.missednotificationsreminder.data.model.util;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

import timber.log.Timber;

/**
 * The application icon picasso handler
 */
public class ApplicationIconHandler extends RequestHandler {
    public static final String SCHEME = "appicon";

    private final PackageManager mPackageManager;

    public ApplicationIconHandler(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return SCHEME.equals(data.uri.getScheme());
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        String packageName = request.uri.getHost();
        return new Result(getAppIcon(packageName), Picasso.LoadedFrom.DISK);
    }

    public Bitmap getAppIcon(String packageName) {

        try {
            Drawable drawable = mPackageManager.getApplicationIcon(packageName);

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable != null) {
                final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            } else {
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e);
        }

        return null;
    }
}
