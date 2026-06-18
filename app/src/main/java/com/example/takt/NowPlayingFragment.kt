package com.example.takt // ТВОЕ ИМЯ ПАКЕТА ОСТАЕТСЯ ЗДЕСЬ!

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class NowPlayingFragment : Fragment() {

    private var musicService: MusicService? = null
    private var isBound = false

    // Элементы интерфейса
    private lateinit var btnPlay: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnPrevious: ImageView
    private lateinit var btnShuffle: TextView // Кнопка RND
    private lateinit var btnRepeat: TextView  // Кнопка RPT

    private lateinit var trackSeekBar: SeekBar
    private lateinit var timeCurrent: TextView
    private lateinit var timeTotal: TextView
    private lateinit var trackTitle: TextView
    private lateinit var trackArtist: TextView
    private lateinit var albumImage: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateSeekBarRunnable: Runnable

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

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

        btnPlay = view.findViewById(R.id.btnPlay)
        btnNext = view.findViewById(R.id.btnNext)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnShuffle = view.findViewById(R.id.btnShuffle)
        btnRepeat = view.findViewById(R.id.btnRepeat)
        trackSeekBar = view.findViewById(R.id.trackSeekBar)
        timeCurrent = view.findViewById(R.id.timeCurrent)
        timeTotal = view.findViewById(R.id.timeTotal)
        trackTitle = view.findViewById(R.id.trackTitle)
        trackArtist = view.findViewById(R.id.trackArtist)
        albumImage = view.findViewById(R.id.albumImage)

        // --- ОБРАБОТКА ОСНОВНЫХ КНОПОК ---

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

        // --- ОБРАБОТКА RND И RPT ---

        btnShuffle.setOnClickListener {
            if (isBound) {
                musicService?.toggleShuffle()
                updateUI() // Перерисовываем цвета
            }
        }

        btnRepeat.setOnClickListener {
            if (isBound) {
                musicService?.toggleRepeat()
                updateUI() // Перерисовываем цвета
            }
        }

        // --- ОБРАБОТКА ПОЛЗУНКА ---

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

        // --- ТАЙМЕР ---

        updateSeekBarRunnable = Runnable {
            if (isBound && musicService?.isPlaying() == true) {
                val currentPos = musicService?.getCurrentPosition() ?: 0
                trackSeekBar.progress = currentPos
                timeCurrent.text = formatTime(currentPos)

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
                albumImage.setImageResource(android.R.color.darker_gray)
            }

            if (ms.isPlaying()) {
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }

            // ПОДКРАШИВАЕМ КНОПКУ RND
            if (ms.isShuffleEnabled) {
                btnShuffle.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            } else {
                btnShuffle.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }

            // ПОДКРАШИВАЕМ КНОПКУ RPT
            if (ms.isRepeatOneEnabled) {
                btnRepeat.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            } else {
                btnRepeat.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }
        }
    }

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

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}