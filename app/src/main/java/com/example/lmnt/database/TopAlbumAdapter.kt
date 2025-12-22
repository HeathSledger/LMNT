package com.example.lmnt.database

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R

class TopAlbumAdapter(private val albums: List<AlbumTimeStat>) :
    RecyclerView.Adapter<TopAlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvAlbumName: TextView = view.findViewById(R.id.tvArtistName) // Wir nutzen das Artist-Layout
        val tvArtistName: TextView = view.findViewById(R.id.tvPlayCount) // Untertitel für den Künstler
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        // Wir verwenden dein existierendes Layout für Top-Listen
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_artist, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.tvRank.text = (position + 1).toString()
        holder.tvAlbumName.text = album.album

        // Rechnet ms in Minuten um für die Anzeige
        val minutes = album.totalTime / (1000 * 60)
        holder.tvArtistName.text = "${album.artist} • $minutes min"
    }

    override fun getItemCount() = albums.size
}