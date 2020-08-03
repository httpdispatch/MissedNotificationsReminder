package com.jakewharton.u2020.ui.bugreport

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.core.app.ShareCompat.IntentBuilder
import com.app.missednotificationsreminder.BuildConfig
import com.jakewharton.u2020.data.LumberYard
import com.jakewharton.u2020.ui.bugreport.BugReportDialog.ReportListener
import com.jakewharton.u2020.util.Intents
import com.jakewharton.u2020.util.Strings
import com.mattprecious.telescope.Lens
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.io.File

/**
 * Pops a dialog asking for more information about the bug report and then creates an email with a
 * JIRA-formatted body.
 */
class BugReportLens(private val context: Activity, private val lumberYard: LumberYard) : Lens(), ReportListener {
    private var screenshot: File? = null
    override fun onCapture(screenshot: File?) {
        this.screenshot = screenshot
        val dialog = BugReportDialog(context)
        dialog.setReportListener(this)
        dialog.show()
    }

    override fun onBugReportSubmit(report: BugReportView.Report) {
        if (report.includeLogs) {
            lumberYard.save()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        Timber.e(e)
                        Toast.makeText(context, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show()
                        submitReport(report, null)
                    }
                    .onEach { submitReport(report, it) }
                    .launchIn(CoroutineScope(Dispatchers.Main))
        } else {
            submitReport(report, null)
        }
    }

    private fun submitReport(report: BugReportView.Report, logs: File?) {
        val dm = context.resources.displayMetrics
        val densityBucket = getDensityString(dm)
        val intent = IntentBuilder.from(context)
                .setType("message/rfc822")
                .addEmailTo("httpdispatch@gmail.com")
                .setSubject(report.title)
        val body = StringBuilder()
        if (!Strings.isBlank(report.description)) {
            body.append("{panel:title=Description}\n").append(report.description).append("\n{panel}\n\n")
        }
        body.append("{panel:title=App}\n")
        body.append("Version: ").append(BuildConfig.VERSION_NAME).append('\n')
        body.append("Version code: ").append(BuildConfig.VERSION_CODE).append('\n')
        body.append("{panel}\n\n")
        body.append("{panel:title=Device}\n")
        body.append("Make: ").append(Build.MANUFACTURER).append('\n')
        body.append("Model: ").append(Build.MODEL).append('\n')
        body.append("Resolution: ")
                .append(dm.heightPixels)
                .append("x")
                .append(dm.widthPixels)
                .append('\n')
        body.append("Density: ")
                .append(dm.densityDpi)
                .append("dpi (")
                .append(densityBucket)
                .append(")\n")
        body.append("Release: ").append(Build.VERSION.RELEASE).append('\n')
        body.append("API: ").append(Build.VERSION.SDK_INT).append('\n')
        body.append("{panel}")
        intent.setText(body.toString())
        if (screenshot != null && report.includeScreenshot) {
            intent.addStream(Uri.fromFile(screenshot))
        }
        if (logs != null) {
            intent.addStream(Uri.fromFile(logs))
        }
        Intents.maybeStartActivity(context, intent.intent)
    }

    companion object {
        private fun getDensityString(displayMetrics: DisplayMetrics): String {
            return when (displayMetrics.densityDpi) {
                DisplayMetrics.DENSITY_LOW -> "ldpi"
                DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
                DisplayMetrics.DENSITY_HIGH -> "hdpi"
                DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
                DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
                DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
                DisplayMetrics.DENSITY_TV -> "tvdpi"
                else -> displayMetrics.densityDpi.toString()
            }
        }
    }

}