package com.vsulimov.resonance.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.MainActivity
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 1002
private const val CHANNEL_ID = "resonance_download_channel"

private const val ACTION_START = "com.vsulimov.resonance.ACTION_START_DOWNLOADS"
private const val ACTION_PAUSE = "com.vsulimov.resonance.ACTION_PAUSE_DOWNLOADS"
private const val ACTION_CANCEL_ALL = "com.vsulimov.resonance.ACTION_CANCEL_ALL_DOWNLOADS"

private const val PROGRESS_UPDATE_THROTTLE_MS = 250L

/** Custom exception to signal that a task was deleted from the database during download. */
private class TaskDeletedException : RuntimeException("Task was removed from database")

/**
 * Foreground service managing background music downloads with notification progress.
 */
class DownloadForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var currentDownloadJob: Job? = null

    @Volatile
    private var isPaused = false

    private lateinit var database: ResonanceDatabase
    private lateinit var storageManager: OfflineStorageManager

    override fun onCreate() {
        super.onCreate()
        val appContainer = applicationContext.getAppContainer()
        database = appContainer.database
        storageManager = appContainer.offlineStorageManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appContainer = applicationContext.getAppContainer()

        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                appContainer.settingsRepository.setDownloadQueuePaused(true)
                currentDownloadJob?.cancel()
                updateNotificationPaused()
            }

            ACTION_CANCEL_ALL -> {
                isPaused = true
                appContainer.settingsRepository.setDownloadQueuePaused(false)
                serviceScope.launch {
                    currentDownloadJob?.cancel()
                    currentDownloadJob?.join()
                    stopSelf()
                }
            }

            ACTION_START, null -> {
                isPaused = false
                appContainer.settingsRepository.setDownloadQueuePaused(false)
                startForegroundWithNotification()
                startDownloadWorker()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDownloadWorker() {
        if (currentDownloadJob?.isActive == true) return

        currentDownloadJob = serviceScope.launch {
            val stuckTasks = database.downloadTaskDao().getTasksByStatus(DownloadStatus.DOWNLOADING)
            stuckTasks.forEach { task ->
                database.downloadTaskDao().updateStatusAndError(
                    id = task.id,
                    status = DownloadStatus.QUEUED,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            }

            while (isActive && !isPaused) {
                val queuedTasks = database.downloadTaskDao().getTasksByStatus(DownloadStatus.QUEUED)
                if (queuedTasks.isEmpty()) {
                    break
                }

                val task = queuedTasks.first()
                val song = database.songDao().getSongById(task.songId)
                val songTitle = song?.title ?: task.songId
                val remainingCount = queuedTasks.size

                updateNotificationDownloading(songTitle, remainingCount)
                database.downloadTaskDao().updateStatusAndError(
                    id = task.id,
                    status = DownloadStatus.DOWNLOADING,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )

                val client: SubsonicClient? = applicationContext.getAppContainer().getAuthenticatedSubsonicClient()
                if (client == null) {
                    database.downloadTaskDao().updateStatusAndError(
                        id = task.id,
                        status = DownloadStatus.FAILED,
                        errorMessage = "Not authenticated",
                        updatedAt = System.currentTimeMillis()
                    )
                    continue
                }

                try {
                    var lastProgressUpdateMs = 0L

                    val downloadResult = client.download(task.songId) { inputStream ->
                        inputStream.use { stream ->
                            storageManager.saveAudioStream(
                                songId = task.songId,
                                suffix = song?.suffix,
                                inputStream = stream,
                                onProgress = { bytesWritten ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastProgressUpdateMs >= PROGRESS_UPDATE_THROTTLE_MS) {
                                        lastProgressUpdateMs = now
                                        val currentTask = database.downloadTaskDao().getTaskById(task.id)
                                        if (currentTask == null) {
                                            throw TaskDeletedException()
                                        }
                                        database.downloadTaskDao().updateProgress(
                                            id = task.id,
                                            status = DownloadStatus.DOWNLOADING,
                                            bytesDownloaded = bytesWritten,
                                            updatedAt = now
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (downloadResult is SubsonicResult.Success) {
                        val savedFile = storageManager.getAudioFile(task.songId, song?.suffix)
                        val finalSize = savedFile.length()

                        if (song != null) {
                            val updatedSong = song.copy(
                                localFilePath = savedFile.absolutePath,
                                downloadedAt = System.currentTimeMillis()
                            )
                            database.songDao().updateSong(updatedSong)

                            song.coverArt?.let { coverId ->
                                if (!storageManager.hasOfflineCover(coverId)) {
                                    try {
                                        client.getCoverArt(id = coverId, size = null) { coverStream ->
                                            coverStream.use { stream ->
                                                storageManager.saveOfflineCoverStream(coverId, stream)
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        if (t is CancellationException) throw t
                                    }
                                }
                            }
                        }

                        database.downloadTaskDao().updateProgress(
                            id = task.id,
                            status = DownloadStatus.DOWNLOADED,
                            bytesDownloaded = finalSize,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        database.downloadTaskDao().updateStatusAndError(
                            id = task.id,
                            status = DownloadStatus.FAILED,
                            errorMessage = "Download stream failed",
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                } catch (_: TaskDeletedException) {
                    continue
                } catch (e: CancellationException) {
                    database.downloadTaskDao().updateStatusAndError(
                        id = task.id,
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    throw e
                } catch (t: Throwable) {
                    database.downloadTaskDao().updateStatusAndError(
                        id = task.id,
                        status = DownloadStatus.FAILED,
                        errorMessage = t.message ?: "Download failed",
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }

            if (!isPaused) {
                stopSelf()
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            title = getString(R.string.download_notification_title_downloading),
            contentText = getString(R.string.download_notification_title_downloading),
            isPaused = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationDownloading(title: String, remainingCount: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "$title ($remainingCount)"
        val notification = buildNotification(
            title = getString(R.string.download_notification_title_downloading),
            contentText = text,
            isPaused = false
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationPaused() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(
            title = getString(R.string.download_notification_title_paused),
            contentText = getString(R.string.download_notification_title_paused),
            isPaused = true
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, contentText: String, isPaused: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createContentPendingIntent())

        if (isPaused) {
            builder.addAction(
                R.mipmap.ic_launcher,
                getString(R.string.downloads_action_resume),
                createPendingIntent(ACTION_START)
            )
        } else {
            builder.addAction(
                R.mipmap.ic_launcher,
                getString(R.string.download_notification_action_pause),
                createPendingIntent(ACTION_PAUSE)
            )
        }

        builder.addAction(
            R.mipmap.ic_launcher,
            getString(R.string.download_notification_action_cancel),
            createPendingIntent(ACTION_CANCEL_ALL)
        )

        return builder.build()
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        /**
         * Starts the download foreground service.
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Pauses active downloads in the service.
         */
        fun pause(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        /**
         * Cancels all queued and active downloads in the service.
         */
        fun cancelAll(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL_ALL
            }
            context.startService(intent)
        }
    }
}
