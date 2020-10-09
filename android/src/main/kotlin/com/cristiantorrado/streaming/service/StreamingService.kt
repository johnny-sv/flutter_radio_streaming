package com.cristiantorrado.streaming.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cristiantorrado.streaming.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import org.greenrobot.eventbus.EventBus


class StreamingService : Service(), Player.EventListener, AudioManager.OnAudioFocusChangeListener, MetadataOutput {

    private var appName = ""
    private var isInitialized = false
    private var url = ""
    private var title = ""
    private var description = ""
    private var notificationColor = ""
    private var stopText = ""
    private var playText = ""
    private var pauseText = ""
    private var playingText = ""
    private var stoppedText = ""
    private var packageIntentName = ""
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentSong = ""

    private var exoPlayer: SimpleExoPlayer? = null
    private var status: PlayerStatus? = null
    private var isLoading: Boolean = false
    private var isPlaying: Boolean = false
    private var telephonyManager: TelephonyManager? = null
    private var notification: Notification? = null
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = SimpleExoPlayer.Builder(this).build()

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(this).build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        requestAudioFocus()
        if (intent?.extras != null) {
            intent.getStringExtra(APP_NAME_EXTRA)?.let { appName = it }
            intent.getStringExtra(URL_EXTRA)?.let { url = it }
            intent.getStringExtra(TITLE_EXTRA)?.let { title = it }
            intent.getStringExtra(DESCRIPTION_EXTRA)?.let { description = it }
            intent.getStringExtra(PLAYING_DESCRIPTION_EXTRA)?.let { playingText = it }
            intent.getStringExtra(STOPPED_DESCRIPTION_EXTRA)?.let { stoppedText = it }
            intent.getStringExtra(PLAY_TEXT_EXTRA)?.let { playText = it }
            intent.getStringExtra(STOP_TEXT_EXTRA)?.let { stopText = it }
            intent.getStringExtra(PAUSE_TEXT_EXTRA)?.let { pauseText = it }
            intent.getStringExtra(COLOR_EXTRA)?.let { notificationColor = it }
            intent.getStringExtra(ANDROID_PACKAGE_TAP_INTENT_EXTRA)?.let { packageIntentName = it }
            if (!isInitialized && intent.action == PLAY_ACTION) {
                val dataSourceFactory = DefaultDataSourceFactory(this, "streaming_service")
                val mediaSource = createLeafMediaSource(Uri.parse(url), "", dataSourceFactory)
                exoPlayer?.prepare(mediaSource)
                isInitialized = true
                exoPlayer?.let {
                    it.addListener(this)
                    it.addMetadataOutput(this)
                    status = getStatusFromPlaybackState(it.playWhenReady, it.playbackState)
                }
            }
        }

        createNotificationChannel()
        notification =  buildNotification(
            title, description, playingText, stoppedText, notificationColor, playText, stopText, pauseText, packageIntentName
        )
        startForeground(NOTIFICATION_ID, notification)
        actionHandler(intent)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        stopStreaming()
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        unregisterReceiver(becomingNoisyReceiver)
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        notification = buildNotification(
            currentSong, description, playingText, stoppedText, notificationColor, playText, stopText, pauseText, packageIntentName
        )?.apply {
            notify(this)
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        getErrorFromExoPlaybackException(error);
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        status = getStatusFromPlaybackState(playWhenReady, playbackState)
        status?.let {
            if (it == PlayerStatus.IDLE) {
                this.notification?.let {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            status = getStatusFromPlaybackState(playWhenReady, playbackState)
            when (status) {
                PlayerStatus.LOADING -> EventBus.getDefault().post(LoadingEvent())
                PlayerStatus.PLAYING -> EventBus.getDefault().post(PlayingEvent())
                PlayerStatus.STOPPED -> EventBus.getDefault().post(StoppedEvent())
                PlayerStatus.PAUSED -> EventBus.getDefault().post(PausedEvent())
                PlayerStatus.IDLE -> { EventBus.getDefault().post(StoppedEvent()) }
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        this.isLoading = isLoading
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                changeVolume(0.75f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseStreaming()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> changeVolume(0.1f)
        }
    }

    override fun onMetadata(metadata: Metadata) {
        var song = ""
        for (n in 0 until metadata.length()) {
            when (val md = metadata[n]) {
                is com.google.android.exoplayer2.metadata.icy.IcyInfo -> {
                    android.util.Log.d("METADATA", "Title: ${md.title} URL: ${md.url}")
                    if (song.isEmpty()) {
                        song = md.title?.trim() ?: ""
                    }
                }
                else -> {
                    android.util.Log.d("METADATA", "Some other sort of metadata: $md")
                }
            }
        }
        currentSong = song

        notification = buildNotification(
            currentSong, description, playingText, stoppedText, notificationColor, playText, stopText, pauseText, packageIntentName
        )?.apply {
            notify(this)
        }

        val event = SongTitleUpdateEvent()
        event.value = currentSong
        EventBus.getDefault().post(event)
    }

    private fun requestAudioFocus() {

        val mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                mAudioManager.requestAudioFocus(it)
            }
        } else {
            mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun removeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusRequest?.let {
                mAudioManager.abandonAudioFocusRequest(it)
            }
        }
    }

    private fun buildNotification(title: String,
                                  description: String,
                                  playingDescription: String,
                                  stoppedDescription: String,
                                  color: String,
                                  playButtonText: String,
                                  stopButtonText: String,
                                  pauseButtonText: String,
                                  packageIntentName: String): Notification? {

        val notificationIntent = Intent(this, this::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val playButtonNotificationText = if (playButtonText.isNotEmpty()) playButtonText else getString(R.string.play_button_title)
        val pauseButtonNotificationText = if (pauseButtonText.isNotEmpty()) pauseButtonText else getString(R.string.pause_button_title)
        val stopButtonNotificationText = if (stopButtonText.isNotEmpty()) stopButtonText else getString(R.string.stop_button_title)

        val finalDescription = description + " - " + if (isPlaying) playingDescription else stoppedDescription
        val playIntent = Intent(this, this::class.java).apply {
            this.action = PLAY_ACTION
        }
        val playAction = PendingIntent.getService(this, PLAY__REQUEST_CODE, playIntent, 0)

        val pauseIntent = Intent(this, this::class.java).apply {
            this.action = PAUSE_ACTION
        }
        val pauseAction = PendingIntent.getService(this, PAUSE_REQUEST_CODE, pauseIntent, 0)

        val stopIntent = Intent(this, this::class.java).apply {
            this.action = STOP_ACTION
        }
        val stopAction = PendingIntent.getService(this, STOP_REQUEST_CODE, stopIntent, 0)

        val closeIntent = Intent(this, this::class.java).apply {
            this.action = CLOSE_ACTION
        }
        val closeAction = PendingIntent.getService(this, CANCEL_SERVICE_REQUEST_CODE, closeIntent, 0)

        var tapAction: PendingIntent? = null
        if (packageIntentName.isNotEmpty() && packageName.isNotEmpty()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                tapAction = PendingIntent.getService(this, START_ACTIVITY_REQUEST_CODE, launchIntent, 0)
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setColorized(true)
            .setContentText(finalDescription)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setDeleteIntent(closeAction)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentIntent(tapAction)
            .addAction(R.drawable.ic_play_arrow_black_24dp, playButtonNotificationText, playAction)
//            .addAction(R.drawable.ic_pause_black_24dp, pauseButtonNotificationText, pauseAction)
            .addAction(R.drawable.ic_stop_black_24dp, stopButtonNotificationText, stopAction)
        try {
            Color.parseColor(color).let {
                notificationBuilder.setColor(it)
            }
        } catch (e: StringIndexOutOfBoundsException) {
            return notificationBuilder.build()
        }
        return notificationBuilder.build()
    }

    private fun notify(notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun actionHandler(intent: Intent?) {
        intent?.let {
            when (it.action) {
                PLAY_ACTION -> {
                    playStreaming()
                }
                STOP_ACTION, CLOSE_ACTION -> {
                    removeAudioFocus()
                    stopStreaming()
                }
                PAUSE_ACTION -> {
                    removeAudioFocus()
                    pauseStreaming()
                }
                GET_CURRENT_SONG_ACTION -> {
                    getCurrentSong()
                }
                else -> {
                }
            }
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pauseStreaming()
        }
    }

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK
                || state == TelephonyManager.CALL_STATE_RINGING) {
                pauseStreaming()
            }
        }
    }

    private fun getCurrentSong() {
        val event = SongTitleUpdateEvent()
        event.value = currentSong
        EventBus.getDefault().post(event)
    }

    private fun stopStreaming() {
        exoPlayer?.let { exoPlayer ->
            if (exoPlayer.isPlaying)
                exoPlayer.stop()
            sendFinishServiceIntent()
        }
    }

    private fun pauseStreaming() {
        exoPlayer?.let { exoPlayer ->
            if (exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = false
            } else {
                sendFinishServiceIntent()
            }
        }
    }

    private fun playStreaming() {
        exoPlayer?.let { exoPlayer ->
            if (!exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = true
            }
        }
    }

    private fun changeVolume(volume: Float) {
        exoPlayer?.let { exoPlayer ->
            if (volume in 0f..1f)
                exoPlayer.volume = volume
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun sendFinishServiceIntent() {
        val myService = Intent(this, this::class.java)
        stopService(myService)
    }

    private fun createLeafMediaSource(
        uri: Uri, extension: String, dataSourceFactory: DefaultDataSourceFactory): MediaSource {
        @C.ContentType val type = Util.inferContentType(uri, extension)
        return when (type) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            else -> throw java.lang.IllegalStateException("Unsupported type: $type")
        }
    }

    private fun getStatusFromPlaybackState(playWhenReady: Boolean, playbackState: Int): PlayerStatus {
        return when (playbackState) {
            Player.STATE_BUFFERING -> PlayerStatus.LOADING
            Player.STATE_ENDED -> PlayerStatus.STOPPED
            Player.STATE_IDLE -> PlayerStatus.IDLE
            Player.STATE_READY -> if (playWhenReady) PlayerStatus.PLAYING else PlayerStatus.PAUSED
            else -> PlayerStatus.IDLE
        }
    }

    private fun getErrorFromExoPlaybackException(error: ExoPlaybackException): PlayerTypeError {
        return when (error.type) {
            ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                PlayerTypeError.TYPE_OUT_OF_MEMORY
            }
            ExoPlaybackException.TYPE_SOURCE -> {
                PlayerTypeError.TYPE_SOURCE
            }
            ExoPlaybackException.TYPE_REMOTE -> {
                PlayerTypeError.TYPE_REMOTE
            }
            ExoPlaybackException.TYPE_RENDERER -> {
                PlayerTypeError.TYPE_RENDERER
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> {
                PlayerTypeError.TYPE_UNEXPECTED
            }
            else -> {
                PlayerTypeError.TYPE_UNEXPECTED
            }
        }
    }

    companion object {
        const val APP_NAME_EXTRA = "APP_NAME_EXTRA"
        const val URL_EXTRA = "URL_EXTRA"
        const val COLOR_EXTRA = "COLOR_EXTRA"
        const val TITLE_EXTRA = "TITLE_EXTRA"
        const val DESCRIPTION_EXTRA = "DESCRIPTION_EXTRA"
        const val STOP_TEXT_EXTRA = "STOP_TEXT_EXTRA"
        const val PAUSE_TEXT_EXTRA = "PAUSE_TEXT_EXTRA"
        const val PLAY_TEXT_EXTRA = "PLAY_TEXT_EXTRA"
        const val PLAYING_DESCRIPTION_EXTRA = "PLAYING_DESCRIPTION_EXTRA"
        const val STOPPED_DESCRIPTION_EXTRA = "STOPPED_DESCRIPTION_EXTRA"
        const val ANDROID_PACKAGE_TAP_INTENT_EXTRA = "ANDROID_PACKAGE_TAP_INTENT_EXTRA"
        const val CURRENT_SONG_TITLE_EXTRA = "CURRENT_SONG_TITLE_EXTRA"

        const val STOP_ACTION = "STOP_ACTION"
        const val PAUSE_ACTION = "PAUSE_ACTION"
        const val PLAY_ACTION = "PLAY_ACTION"
        const val CLOSE_ACTION = "CLOSE_ACTION"
        const val GET_CURRENT_SONG_ACTION = "GET_CURRENT_SONG_ACTION"

        const val CHANNEL_ID = "PLAYER_CHANNEL_ID"
        const val CHANNEL_NAME = "Player Notification Channel"
        const val PLAY__REQUEST_CODE = 1
        const val PAUSE_REQUEST_CODE = 2
        const val STOP_REQUEST_CODE = 3
        const val CANCEL_SERVICE_REQUEST_CODE = 4
        const val START_ACTIVITY_REQUEST_CODE = 5
        const val NOTIFICATION_ID = 1602246405
    }


}