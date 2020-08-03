package com.jakewharton.u2020.data

import android.app.Application
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import okio.buffer
import okio.sink
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.temporal.ChronoField
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LumberYard @Inject constructor(private val app: Application) {
    private val entries: Deque<Entry> = ArrayDeque(BUFFER_SIZE + 1)
    private val entrySubject = BroadcastChannel<Entry>(2000)
    fun tree(): Timber.Tree {
        val scope = CoroutineScope(Executors
                .newSingleThreadExecutor { runnable -> Thread(runnable, "LumberYard") }
                .asCoroutineDispatcher())
        return object : DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                scope.launch { addEntry(Entry(LocalDateTime.now(), priority, tag ?: "", message)) }
            }
        }
    }

    @Synchronized
    private suspend fun addEntry(entry: Entry) {
        entries.addLast(entry)
        if (entries.size > BUFFER_SIZE) {
            entries.removeFirst()
        }
        entrySubject.send(entry)
    }

    fun bufferedLogs(): List<Entry> {
        return ArrayList(entries)
    }

    fun logs(): Flow<Entry> {
        return entrySubject.asFlow()
    }

    /**
     * Save the current logs to disk.
     */
    fun save(): Flow<File> = flow {
        val folder = app.getExternalFilesDir(null)
                ?: throw IOException("External storage is not mounted.")
        var fileName = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + ".txt"
        // replace ':' char to avoid file saving issue on some devices
        fileName = fileName.replace(":".toRegex(), "_")
        val output = File(folder, fileName)
        output.sink().buffer()
                .use {
                    val entries = bufferedLogs()
                    for (entry in entries) {
                        it.writeUtf8(entry.prettyPrint()).writeByte('\n'.toInt())
                    }
                    it.close()
                    emit(output)
                }
    }

    /**
     * Delete all of the log files saved to disk. Be careful not to call this before any intents have
     * finished using the file reference.
     */
    fun cleanUp() {
        GlobalScope.launch(Dispatchers.IO) {
            Timber.d("cleanUp()")
            val folder = app.getExternalFilesDir(null)
            if (folder != null) {
                val files = folder.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.endsWith(".log")) {
                            file.delete()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 10000
    }

    data class Entry(val time: LocalDateTime, val level: Int, val tag: String, val message: String) {
        fun prettyPrint(): String {
            return String.format("%s %22s %s %s", displayTime(), tag, displayLevel(),  // Indent newlines to match the original indentation.
                    message.replace("\\n".toRegex(), "\n                         "))
        }

        fun displayLevel(): String {
            return when (level) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
        }

        fun displayTime(): String {
            return formatter.format(time)
        }

        companion object {
            val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
                    .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalStart()
                    .appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
                    .toFormatter()
        }

    }
}