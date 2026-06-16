package com.example.takt

import android.app.PendingIntent
import com.example.takt.MainActivity
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "takt_music_channel"

    // Главная фишка для системного плеера
    private lateinit var mediaSession: MediaSession

    // Сохраняем данные трека, чтобы Фрагмент мог их забрать
    var trackTitle: String = "UNKNOWN_TITLE"
    var trackArtist: String = "UNKNOWN_ARTIST"
    var trackArt: Bitmap? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Инициализируем Медиа-сессию
        mediaSession = MediaSession(this, "tAkt_Session")

        // функционал для фонового режима(кнопки в шторке, нажатия на наушниках)
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                play() // вызов функции
            }

            override fun onPause() {
                pause() // тоже вызов функции
            }

            override fun onSeekTo(pos: Long) {
                seekTo(pos.toInt())
            }
        })

        // ВАЖНО: Укажи тут свой файл
        mediaPlayer = MediaPlayer.create(this, R.raw.feuer_und_wasser)

        // Вытаскиваем метаданные прямо при запуске сервиса
        extractMetadata()
    }

    private fun extractMetadata() {
        val retriever = MediaMetadataRetriever()
        try {
            val afd = resources.openRawResourceFd(R.raw.feuer_und_wasser) // И ТУТ ТОЖЕ СВОЙ ФАЙЛ
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

            trackTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "UNKNOWN_TITLE"
            trackArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "UNKNOWN_ARTIST"

            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                trackArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            }
            afd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        // Передаем данные в систему Android (для шторки и экрана блокировки)
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, trackTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, trackArtist)

        if (trackArt != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, trackArt)
        }
        mediaSession.setMetadata(metadataBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun play() {
        mediaPlayer?.start()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    @SuppressLint("MissingPermission")
    fun pause() {
        mediaPlayer?.pause()
        updatePlaybackState(PlaybackState.STATE_PAUSED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    // Сообщаем системе, играет ли музыка прямо сейчас
    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0L, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
        mediaSession.isActive = true
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun seekTo(position: Int) { mediaPlayer?.seekTo(position) }

    // Строим красивое уведомление формата MediaStyle
    private fun buildNotification(): Notification {
        // инструкция: Если нажали на медиаплеер в шторке, то открой MainActivity
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Создаем развилку для версий Android
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        // Подключаем стиль системного плеера
        val style = Notification.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)

        builder.setStyle(style)
            .setContentTitle(trackTitle)
            .setContentText(trackArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent) // прикрепление инструкции
            .setOngoing(isPlaying())

        if (trackArt != null) {
            builder.setLargeIcon(trackArt)
        }

        return builder.build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "tAkt Music Playback", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
        super.onDestroy()
    }
}
