package com.example.takt

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Field // импорт что бы высосать все треки из папки raw

class CrzcFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crzc, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAlbums)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val trackList = mutableListOf<Track>()
        val retriever = MediaMetadataRetriever()
        val packageName = requireContext().packageName

        // --- МАГИЯ РЕФЛЕКСИИ ---
        // Получаем массив всех переменных, которые лежат в папке raw
        val rawFields: Array<Field> = R.raw::class.java.fields

        for (field in rawFields) {
            try {
                // Достаем ID каждого файла
                val rawId = field.getInt(null)
                val uriPath = "android.resource://$packageName/$rawId"

                retriever.setDataSource(requireContext(), Uri.parse(uriPath))

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Crazy Cucumber"
                val albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Singles"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

                trackList.add(Track(title, artist, formatTime(durationMs.toInt()), uriPath, albumName))
            } catch (e: Exception) {
                // Если в raw попадет не аудиофайл, этот блок просто проигнорирует ошибку
                e.printStackTrace()
            }
        }
        retriever.release()
        // ------------------------

        // АВТОМАТИЧЕСКАЯ ГРУППИРОВКА ПО АЛЬБОМАМ
        val albumsMap = trackList.groupBy { it.album }
        val albumsList = albumsMap.map { (albumName, tracks) ->
            Album(
                title = albumName,
                artist = tracks.first().artist,
                coverPath = tracks.first().path,
                tracks = tracks
            )
        }.sortedBy { it.title }

        // ОТПРАВЛЯЕМ В АДАПТЕР
        val albumAdapter = AlbumAdapter(albumsList) { clickedAlbum ->
            val paths = ArrayList(clickedAlbum.tracks.map { it.path })
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                action = "PLAY_NEW_TRACK"
                putStringArrayListExtra("TRACK_LIST", paths)
                putExtra("TRACK_INDEX", 0)
            }
            requireContext().startService(intent)
        }

        recyclerView.adapter = albumAdapter
        return view
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}