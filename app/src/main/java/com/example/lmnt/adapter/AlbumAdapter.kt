package com.example.lmnt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lmnt.R
import com.example.lmnt.model.Album

class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAlbumArt: ImageView = view.findViewById(R.id.ivAlbumArt)
        val tvAlbumTitle: TextView = view.findViewById(R.id.tvAlbumTitle)
        val tvAlbumArtist: TextView = view.findViewById(R.id.tvAlbumArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]

        holder.tvAlbumTitle.text = album.title
        holder.tvAlbumArtist.text = album.artist

        // Album Cover laden
        holder.ivAlbumArt.load(album.artworkUri) {
            crossfade(true)
            placeholder(R.drawable.ic_music_note)
            error(R.drawable.ic_music_note)
        }

        holder.itemView.setOnClickListener { onAlbumClick(album) }
    }

    override fun getItemCount() = albums.size
}