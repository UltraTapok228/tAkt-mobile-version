package com.example.takt

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class NowPlayingFragment : Fragment() {

    private var musicService: MusicService? = null
    private var isBound = false

    // Элементы интерфейса
    private lateinit var btnPlay: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnPrevious: ImageView
    private lateinit var trackSeekBar: SeekBar
    private lateinit var timeCurrent: TextView
    private lateinit var timeTotal: TextView
    private lateinit var trackTitle: TextView
    private lateinit var trackArtist: TextView
    private lateinit var albumImage: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateSeekBarRunnable: Runnable

    // Подключение к нашему сервису
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // Обновляем весь интерфейс при подключении
            updateUI()

            if (musicService?.isPlaying() == true) {
                handler.post(updateSeekBarRunnable)
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)

        // Привязываем переменные к XML-элементам
        btnPlay = view.findViewById(R.id.btnPlay)
        btnNext = view.findViewById(R.id.btnNext)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        trackSeekBar = view.findViewById(R.id.trackSeekBar)
        timeCurrent = view.findViewById(R.id.timeCurrent)
        timeTotal = view.findViewById(R.id.timeTotal)
        trackTitle = view.findViewById(R.id.trackTitle)
        trackArtist = view.findViewById(R.id.trackArtist)
        albumImage = view.findViewById(R.id.albumImage)

        // --- ОБРАБОТКА КЛИКОВ ПО КНОПКАМ ---

        btnPlay.setOnClickListener {
            if (isBound) {
                if (musicService?.isPlaying() == true) {
                    musicService?.pause()
                    updateUI()
                    handler.removeCallbacks(updateSeekBarRunnable)
                } else {
                    musicService?.play()
                    updateUI()
                    handler.post(updateSeekBarRunnable)
                }
            }
        }

        btnNext.setOnClickListener {
            if (isBound) {
                musicService?.skipToNext()
                updateUI()
            }
        }

        btnPrevious.setOnClickListener {
            if (isBound) {
                musicService?.skipToPrevious()
                updateUI()
            }
        }

        // --- ОБРАБОТКА ПОЛЗУНКА ПЕРЕМОТКИ ---

        trackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isBound) {
                    musicService?.seekTo(progress)
                    timeCurrent.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- ТАЙМЕР ОБНОВЛЕНИЯ ИНТЕРФЕЙСА ---

        updateSeekBarRunnable = Runnable {
            if (isBound && musicService?.isPlaying() == true) {
                val currentPos = musicService?.getCurrentPosition() ?: 0
                trackSeekBar.progress = currentPos
                timeCurrent.text = formatTime(currentPos)

                // Если песня переключилась автоматически, название в сервисе поменяется.
                // Заметили это -> обновляем картинки и тексты!
                if (trackTitle.text != musicService?.trackTitle) {
                    updateUI()
                }

                handler.postDelayed(updateSeekBarRunnable, 1000)
            }
        }

        return view
    }

    // --- ФУНКЦИЯ ОБНОВЛЕНИЯ ГРАФИКИ ---
    private fun updateUI() {
        musicService?.let { ms ->
            trackSeekBar.max = ms.getDuration()
            timeTotal.text = formatTime(ms.getDuration())

            trackTitle.text = ms.trackTitle
            trackArtist.text = ms.trackArtist

            if (ms.trackArt != null) {
                albumImage.setImageBitmap(ms.trackArt)
            } else {
                albumImage.setImageResource(android.R.color.darker_gray) // Заглушка, если нет обложки
            }

            if (ms.isPlaying()) {
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }

    // --- ПОДКЛЮЧЕНИЕ / ОТКЛЮЧЕНИЕ СЕРВИСА ---

    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext(), MusicService::class.java)
        requireActivity().startService(intent)
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            requireActivity().unbindService(connection)
            isBound = false
        }
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    // Вспомогательная функция для перевода миллисекунд в минуты:секунды
    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
