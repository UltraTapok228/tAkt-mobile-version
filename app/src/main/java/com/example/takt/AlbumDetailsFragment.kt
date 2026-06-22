package com.example.takt

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlbumDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album_details, container, false)

        val coverImage = view.findViewById<ImageView>(R.id.detailsCover)
        val titleText = view.findViewById<TextView>(R.id.detailsTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAlbumTracks)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackToAlbums)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Кнопка возврата к сетке альбомов
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Получаем переданные данные альбома
        val albumTitle = arguments?.getString("ALBUM_TITLE") ?: "Unknown"
        val coverPath = arguments?.getString("COVER_PATH") ?: ""
        val tracks = arguments?.getSerializable("TRACKS") as? ArrayList<Track> ?: arrayListOf()

        titleText.text = albumTitle

        if (coverPath.isNotEmpty()) {
            Thread {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(requireContext(), Uri.parse(coverPath))
                    val artBytes = retriever.embeddedPicture
                    if (artBytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                        coverImage.post { coverImage.setImageBitmap(bitmap) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }.start()
        }

        // Выводим треки через твой стандартный адаптер
        val trackAdapter = TrackAdapter(tracks) { clickedTrack ->
            val paths = ArrayList(tracks.map { it.path })
            val clickedIndex = tracks.indexOf(clickedTrack)

            val intent = Intent(requireContext(), MusicService::class.java).apply {
                action = "PLAY_NEW_TRACK"
                putStringArrayListExtra("TRACK_LIST", paths)
                putExtra("TRACK_INDEX", clickedIndex)
            }
            requireContext().startService(intent)
        }
        recyclerView.adapter = trackAdapter

        return view
    }

    companion object {
        fun newInstance(title: String, coverPath: String, tracks: ArrayList<Track>): AlbumDetailsFragment {
            return AlbumDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("ALBUM_TITLE", title)
                    putString("COVER_PATH", coverPath)
                    putSerializable("TRACKS", tracks)
                }
            }
        }
    }
}