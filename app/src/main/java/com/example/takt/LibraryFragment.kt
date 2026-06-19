package com.example.takt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LibraryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter

    // Специальная штука для запроса разрешений в новых версиях Android
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Если пользователь нажал "Разрешить", запускаем сканирование
            loadTracksFromDevice()
        } else {
            Toast.makeText(requireContext(), "Нужен доступ к памяти для поиска музыки", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewTracks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // При открытии экрана проверяем разрешения
        checkPermissionsAndLoad()

        return view
    }

    private fun checkPermissionsAndLoad() {
        // Для Android 13+ используем одно разрешение, для старых - другое
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение уже есть (давали ранее) -> просто грузим треки
            loadTracksFromDevice()
        } else {
            // Запрашиваем разрешение (появится всплывающее окно)
            requestPermissionLauncher.launch(permission)
        }
    }

    // --- ФУНКЦИЯ СКАНИРОВАНИЯ ТЕЛЕФОНА ---
    private fun loadTracksFromDevice() {
        val trackList = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Какие колонки вытаскиваем из системной базы данных
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA, // Это физический путь к файлу (.mp3/.wav)
            MediaStore.Audio.Media.ALBUM
        )

        // Жесткое условие: файл должен быть МУЗЫКОЙ (отсеиваем рингтоны и голосовухи)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = requireContext().contentResolver.query(uri, projection, selection, null, null)

        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (it.moveToNext()) {
                val title = it.getString(titleColumn) ?: "UNKNOWN_TITLE"
                val artist = it.getString(artistColumn) ?: "UNKNOWN_ARTIST"
                val albumName = it.getString(albumCol) ?: "UNKNOWN_ALBUM"
                val durationMs = it.getLong(durationColumn)
                val path = it.getString(dataColumn)

                // Сначала отсеиваем слишком короткие треки
                if (durationMs < 1000) continue

                // Добавляем трек в список ровно один раз!
                trackList.add(Track(title, artist, formatTime(durationMs.toInt()), path, albumName))
            }
        }

        // Обновляем наш список на экране и ловим клики
        trackAdapter = TrackAdapter(trackList) { clickedTrack ->
            // 1. Собираем массив путей всех треков из списка
            val paths = ArrayList(trackList.map { it.path })
            // 2. Находим индекс того трека, на который нажали
            val clickedIndex = trackList.indexOf(clickedTrack)

            val intent = android.content.Intent(requireContext(), MusicService::class.java).apply {
                action = "PLAY_NEW_TRACK"
                putStringArrayListExtra("TRACK_LIST", paths) // Передаем весь плейлист
                putExtra("TRACK_INDEX", clickedIndex) // Передаем индекс текущего
            }
            requireContext().startService(intent)
        }

        recyclerView.adapter = trackAdapter

    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
