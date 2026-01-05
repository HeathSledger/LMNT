package com.example.lmnt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lmnt.R
import com.example.lmnt.Song


class RecentlyAddedAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<RecentlyAddedAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivArt: ImageView = view.findViewById(R.id.ivAlbumArt)
        val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvSongArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recently_added, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        // Hier nutzen wir Glide oder Coil zum Laden des Covers
        Glide.with(holder.ivArt.context)
            .load(song.artworkUri)
            .placeholder(R.drawable.ic_music_note)
            .into(holder.ivArt)

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount() = songs.size
}