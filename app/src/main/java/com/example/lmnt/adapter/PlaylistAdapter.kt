package com.example.lmnt.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R

class PlaylistAdapter(
    private val items: List<PlaylistItem>,
    private val onItemClick: (PlaylistItem) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivPlaylistIcon)
        val name: TextView = view.findViewById(R.id.tvPlaylistName)
        val count: TextView = view.findViewById(R.id.tvPlaylistSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.count.text = "${item.songCount} Songs"

        // Setze das Icon: Gefülltes Herz für Favoriten, sonst Musik-Ordner
        if (item.isFavoriteType) {
            holder.icon.setImageResource(R.drawable.heart_filled)
            // Optional: Das Herz rot färben, falls gewünscht
            // holder.icon.setColorFilter(android.graphics.Color.RED)
        } else {
            holder.icon.setImageResource(R.drawable.playlist) // Dein importiertes Icon
            holder.icon.clearColorFilter()
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}