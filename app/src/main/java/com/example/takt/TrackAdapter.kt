package com.example.takt

// этот файл нужен для распределения данных

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(private val trackList: List<Track>) :
    RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    // 1. Ищем нужные текстовые поля внутри макета item_track
    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.itemTrackTitle)
        val artistText: TextView = itemView.findViewById(R.id.itemTrackArtist)
        val durationText: TextView = itemView.findViewById(R.id.itemTrackDuration)
    }

    // 2. Создаем пустую "формочку" (превращаем XML в программный объект)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    // 3. Берем данные конкретного трека и вписываем их в поля
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val currentTrack = trackList[position]
        holder.titleText.text = currentTrack.title
        holder.artistText.text = currentTrack.artist
        holder.durationText.text = currentTrack.duration
    }

    // 4. Сообщаем списку, сколько всего у нас треков
    override fun getItemCount(): Int {
        return trackList.size
    }
}