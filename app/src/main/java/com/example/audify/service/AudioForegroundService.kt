package com.example.audify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.audify.service.ProximitySensorManager
import com.example.audify.service.ShakeDetector
import androidx.core.app.NotificationCompat
import com.example.audify.MainActivity
import com.example.audify.R

class AudioForegroundService : Service() {

    companion object {
        const val TAG = "AudioForegroundService"
        const val CHANNEL_ID = "audify_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.audify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.audify.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.audify.ACTION_TOGGLE"
        const val ACTION_STOP = "com.example.audify.ACTION_STOP"
        const val ACTION_SEEK_FORWARD = "com.example.audify.ACTION_SEEK_FORWARD"
        const val ACTION_SEEK_REWIND = "com.example.audify.ACTION_SEEK_REWIND"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"

        var isServiceRunning = false
            private set
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var powerManager: PowerManager? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var proximitySensor: ProximitySensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var currentTitle = ""
    private var currentUrl = ""
    private var useEarpiece = false
    private var hasStartedForeground = false

    var onPreparedListener: ((Int) -> Unit)? = null
    var onCompletionListener: (() -> Unit)? = null
    var onErrorListener: ((String) -> Unit)? = null
    var onPlayStateChanged: ((Boolean) -> Unit)? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0

    val duration: Int
        get() = mediaPlayer?.duration ?: 0

    inner class LocalBinder : Binder() {
        fun getService(): AudioForegroundService = this@AudioForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        createNotificationChannel()
        isServiceRunning = true
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_PAUSE -> pause()
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                if (url != null && (currentUrl != url || mediaPlayer == null)) {
                    currentUrl = url
                    prepareAndPlay(url, title)
                } else {
                    if (!hasStartedForeground) startForeground(NOTIFICATION_ID, buildNotification())
                    resume()
                }
            }
            ACTION_SEEK_FORWARD -> seekRelative(10000)
            ACTION_SEEK_REWIND -> seekRelative(-10000)
            ACTION_STOP -> {
                stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val url = intent?.getStringExtra(EXTRA_URL)
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: ""
                if (url != null) {
                    currentUrl = url
                    prepareAndPlay(url, title)
                }
            }
        }
        return START_NOT_STICKY
    }

    fun prepareAndPlay(url: String, title: String) {
        currentTitle = title
        Log.d(TAG, "prepareAndPlay: $url")

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)
            setOnPreparedListener { mp ->
                Log.d(TAG, "MediaPlayer prepared, duration=${mp.duration}")

                startForeground(NOTIFICATION_ID, buildNotification())
                hasStartedForeground = true

                startSensors()
                mp.start()
                onPreparedListener?.invoke(mp.duration)
                onPlayStateChanged?.invoke(true)
                updateNotification()
            }
            setOnCompletionListener {
                Log.d(TAG, "MediaPlayer completed")
                onCompletionListener?.invoke()
                onPlayStateChanged?.invoke(false)
                updateNotification()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                onErrorListener?.invoke("Error de reproducción")
                onPlayStateChanged?.invoke(false)
                true
            }
            prepareAsync()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) pause() else resume()
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                onPlayStateChanged?.invoke(false)
                updateNotification()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                onPlayStateChanged?.invoke(true)
                updateNotification()
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun seekRelative(millis: Int) {
        mediaPlayer?.let { mp ->
            val newPos = (mp.currentPosition + millis).coerceIn(0, mp.duration)
            mp.seekTo(newPos)
        }
    }

    fun setEarpieceMode(useEarpiece: Boolean) {
        if (useEarpiece == this.useEarpiece) return
        this.useEarpiece = useEarpiece
        val am = audioManager ?: return
        if (useEarpiece) {
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            acquireProximityWakeLock()
            Log.d(TAG, "Switched to earpiece")
        } else {
            am.mode = AudioManager.MODE_NORMAL
            am.isSpeakerphoneOn = true
            releaseProximityWakeLock()
            Log.d(TAG, "Switched to speaker")
        }
    }

    private fun acquireProximityWakeLock() {
        if (proximityWakeLock == null) {
            proximityWakeLock = powerManager?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "Audify:Proximity"
            )
        }
        if (proximityWakeLock?.isHeld == false) {
            proximityWakeLock?.acquire()
            Log.d(TAG, "Proximity wake lock acquired")
        }
    }

    private fun releaseProximityWakeLock() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock?.release()
            Log.d(TAG, "Proximity wake lock released")
        }
    }

    private fun startSensors() {
        if (proximitySensor != null) return
        proximitySensor = ProximitySensorManager(this).apply {
            onNear = { setEarpieceMode(true) }
            onFar = { setEarpieceMode(false) }
            start()
        }
        shakeDetector = ShakeDetector(this).apply {
            onDoubleShake = { togglePlayPause() }
            start()
        }
        Log.d(TAG, "Sensors started")
    }

    private fun stopSensors() {
        proximitySensor?.stop()
        proximitySensor = null
        shakeDetector?.stop()
        shakeDetector = null
        Log.d(TAG, "Sensors stopped")
    }

    fun stop() {
        stopSensors()
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = ""
        currentTitle = ""
        hasStartedForeground = false
        releaseProximityWakeLock()
        proximityWakeLock = null
        isServiceRunning = false
        audioManager?.let {
            it.mode = AudioManager.MODE_NORMAL
            it.isSpeakerphoneOn = true
        }
        Log.d(TAG, "Service stopped")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproducción de podcast",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de reproducción de Audify"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = PendingIntent.getService(
            this, 0,
            Intent(this, AudioForegroundService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rewindAction = PendingIntent.getService(
            this, 1,
            Intent(this, AudioForegroundService::class.java).apply { action = ACTION_SEEK_REWIND },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardAction = PendingIntent.getService(
            this, 2,
            Intent(this, AudioForegroundService::class.java).apply { action = ACTION_SEEK_FORWARD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = PendingIntent.getService(
            this, 3,
            Intent(this, AudioForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_audify_logo)
            .setContentTitle(currentTitle.ifEmpty { "Audify" })
            .setContentText(if (isPlaying) "Reproduciendo..." else "Pausado")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_rewind_10, "Retroceder", rewindAction)
            .addAction(playPauseIcon, if (isPlaying) "Pausar" else "Reproducir", toggleAction)
            .addAction(R.drawable.ic_forward_10, "Adelantar", forwardAction)
            .addAction(R.drawable.ic_stop, "Detener", stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}
