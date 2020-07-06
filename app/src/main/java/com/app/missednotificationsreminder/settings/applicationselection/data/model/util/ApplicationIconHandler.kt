package com.app.missednotificationsreminder.settings.applicationselection.data.model.util

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import timber.log.Timber

/**
 * The application icon picasso handler
 */
class ApplicationIconHandler(private val mPackageManager: PackageManager) : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        return SCHEME == data.uri.scheme
    }

    override fun load(request: Request, networkPolicy: Int): Result? {
        return getAppIcon(request.uri.host)?.let {
            Result(it, Picasso.LoadedFrom.DISK)
        }
    }

    private fun getAppIcon(packageName: String?): Bitmap? {
        try {
            return mPackageManager.getApplicationIcon(packageName)?.let { drawable ->
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Throwable) {
            Timber.e(e)
        }
        return null
    }

    companion object {
        const val SCHEME = "appicon"
    }

}