package com.example.takt

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlbumAdapter(
    private val albumList: List<Album>,
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView = itemView.findViewById(R.id.albumCover)
        val titleText: TextView = itemView.findViewById(R.id.albumTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val currentAlbum = albumList[position]
        holder.titleText.text = currentAlbum.title

        holder.coverImage.setImageResource(android.R.color.darker_gray)

        // Достаем обложку прямо из MP3 файла
        Thread {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(currentAlbum.coverPath)
                retriever.setDataSource(holder.itemView.context, uri)
                val artBytes = retriever.embeddedPicture

                if (artBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    holder.coverImage.post { holder.coverImage.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }.start()

        holder.itemView.setOnClickListener {
            onAlbumClick(currentAlbum)
        }
    }

    override fun getItemCount(): Int = albumList.size
}