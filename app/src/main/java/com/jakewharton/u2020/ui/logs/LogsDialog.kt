package com.jakewharton.u2020.ui.logs

import android.content.Context
import android.content.DialogInterface
import android.os.SystemClock
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.app.missednotificationsreminder.databinding.DebugLogsBinding
import com.app.missednotificationsreminder.ui.widget.dialog.LifecycleAlertDialog
import com.app.missednotificationsreminder.util.ShareUtils
import com.app.missednotificationsreminder.util.flow.bufferTimeout
import com.jakewharton.u2020.data.LumberYard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LogsDialog(context: Context,
                 private val lumberYard: LumberYard,
                 private val parentLifecycleOwner: LifecycleOwner) : LifecycleAlertDialog(context) {
    private val adapter: LogAdapter = LogAdapter(context)
    private val query: EditText

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                filterData(query.text, lumberYard.bufferedLogs(), false)
            }
            adapter.setLogs(data)
            lumberYard.logs()
                    .bufferTimeout(duration = 2000)
                    .filter { it.isNotEmpty() }
                    .map { filterData(query.text, it, true) }
                    .filter { it.isNotEmpty() }
                    .flowOn(Dispatchers.Default)
                    .buffer()
                    .onEach { adapter.addLogs(it) }
                    .launchIn(lifecycleScope)
            callbackFlow<String> {
                val listener = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        offer(s?.toString() ?: "")
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    }

                }
                query.addTextChangedListener(listener)
                awaitClose {
                    Timber.d("close")
                    query.removeTextChangedListener(listener)
                }
            }
                    .debounce(1000)
                    .conflate()
                    .map { filterData(it, lumberYard.bufferedLogs(), false) }
                    .flowOn(Dispatchers.Default)
                    .onEach { adapter.setLogs(it) }
                    .launchIn(lifecycleScope)
        }

    }

    private fun filterData(query: CharSequence, entries: List<LumberYard.Entry>, silent: Boolean): List<LumberYard.Entry> {
        if (!silent) {
            Timber.d("filterData() called in thread=${Thread.currentThread().name}")
        }
        if (TextUtils.isEmpty(query)) {
            return entries
        }
        if (!silent) {
            Timber.d("filterData() called with: query = %s;",
                    query)
        }
        val start = SystemClock.elapsedRealtime()
        val result: MutableList<LumberYard.Entry> = ArrayList()
        var pattern: Pattern? = null
        try {
            pattern = Pattern.compile(query.toString(), Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            Timber.e("Invalid pattern: %s", query)
        }
        if (pattern != null) {
            for (entry in entries) {
                for (value in arrayOf(
                        entry.displayLevel(),
                        entry.displayTime(),
                        entry.tag,
                        entry.message
                )) {
                    if (pattern.matcher(value).find()) {
                        result.add(entry)
                        break
                    }
                }
            }
        }
        if (!silent) {
            Timber.d("filterData: duration %d", SystemClock.elapsedRealtime() - start)
        }
        return result
    }

    private suspend fun share() {
        lumberYard.save() //
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    Timber.e(e)
                    Toast.makeText(context, "Couldn't save the logs for sharing.", Toast.LENGTH_SHORT)
                            .show()
                }
                .collect { ShareUtils.shareFile(ShareUtils.getAppFileProviderUri(it, context), context) }
    }

    init {
        val binding = DebugLogsBinding.inflate(LayoutInflater.from(context))
        with(binding.list) {
            transcriptMode = ListView.TRANSCRIPT_MODE_NORMAL
            adapter = this@LogsDialog.adapter
        }
        query = binding.query
        setTitle("Logs")
        setView(binding.root)
        setButton(DialogInterface.BUTTON_NEGATIVE, "Close") { _, _ -> }
        setButton(DialogInterface.BUTTON_POSITIVE, "Share") { _, _ -> parentLifecycleOwner.lifecycleScope.launch { share() } }
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }
}