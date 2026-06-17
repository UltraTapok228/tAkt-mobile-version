package com.example.takt

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

    private var trackPaths = ArrayList<String>()
    private var currentIndex = 0

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

        // Инициализируем Медиа-сессию(простыми словами шторка и прочее)
        mediaSession = MediaSession(this, "tAkt_Session")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            override fun onSkipToNext() { skipToNext() } // кнопка дальше
            override fun onSkipToPrevious() { skipToPrevious() } // кнопка назад
        })


        // Создаем абсолютно пустой плеер при запуске (никаких R.raw.track)
        mediaPlayer = MediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "PLAY_NEW_TRACK") {
            val list = intent.getStringArrayListExtra("TRACK_LIST")
            val index = intent.getIntExtra("TRACK_INDEX", 0)

            if (list != null && list.isNotEmpty()) {
                trackPaths = list
                currentIndex = index
                playNewTrack(trackPaths[currentIndex])
            }
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // --- ФУНКЦИЯ ДЛЯ ЗАПУСКА ЛЮБОГО ТРЕКА ПО ПУТИ ---
    private fun playNewTrack(path: String) {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(path)
            mediaPlayer?.prepare()

            // автоматическое переключение треков
            mediaPlayer?.setOnCompletionListener {
                skipToNext() // Как только трек закончился - включаем следующий
            }

            extractMetadata(path)
            play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun skipToNext() {
        if (trackPaths.isEmpty()) return
        // Увеличиваем индекс, если дошли до конца - переходим в начало (зацикливание плейлиста)
        currentIndex = (currentIndex + 1) % trackPaths.size
        playNewTrack(trackPaths[currentIndex])
    }

    fun skipToPrevious() {
        if (trackPaths.isEmpty()) return
        // Уменьшаем индекс, если ушли в минус - переходим в конец
        currentIndex = if (currentIndex - 1 < 0) trackPaths.size - 1 else currentIndex - 1
        playNewTrack(trackPaths[currentIndex])
    }


    private fun extractMetadata(path: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path) // Читаем физический файл с телефона

            trackTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "UNKNOWN_TITLE"
            trackArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "UNKNOWN_ARTIST"

            val artBytes = retriever.embeddedPicture
            trackArt = if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null // Если картинки нет, обнуляем старую
            }
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

    // --- УПРАВЛЕНИЕ ВОСПРОИЗВЕДЕНИЕМ ---

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

    // --- СОЗДАНИЕ СИСТЕМНОГО УВЕДОМЛЕНИЯ ДЛЯ ШТОРКИ ---

    private fun buildNotification(): Notification {
        // Создаем инструкцию: "Если на меня нажали, открой MainActivity"
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Развилка для старых/новых Android
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
            .setContentIntent(pendingIntent) // Прикрепляем инструкцию перехода в приложение
            .setOngoing(isPlaying())

        if (trackArt != null) {
            builder.setLargeIcon(trackArt)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "tAkt Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
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
