package com.example.takt // Твой пакет!

// этот файл нужен для распределения данных

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onTrackClick - функция, которая сработает при нажатии
class TrackAdapter(
    private val trackList: List<Track>,
    private val onTrackClick: (Track) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.itemTrackTitle)
        val artistText: TextView = itemView.findViewById(R.id.itemTrackArtist)
        val durationText: TextView = itemView.findViewById(R.id.itemTrackDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val currentTrack = trackList[position]
        holder.titleText.text = currentTrack.title
        holder.artistText.text = currentTrack.artist
        holder.durationText.text = currentTrack.duration

        // ВЕШАЕМ СЛУШАТЕЛЬ НА ВСЮ СТРОКУ ТРЕКА
        holder.itemView.setOnClickListener {
            onTrackClick(currentTrack) // Передаем кликнутый трек наружу
        }
    }

    override fun getItemCount(): Int = trackList.size
}
