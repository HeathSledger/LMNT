package com.example.lmnt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongsAdapter(
    private val songs: List<Song>,
    private val showTrackNumber: Boolean = false,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTrackNumber: TextView = view.findViewById(R.id.tvTrackNumber)
        val tvTitle: TextView = view.findViewById(R.id.songTitle)
        val tvArtist: TextView = view.findViewById(R.id.songArtist)
        val ivCover: ImageView = view.findViewById(R.id.ivSongCover) // ImageView hier definieren
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        // 1. Cover laden mit Glide
        // FIX: Nutze eine echte Drawable-Ressource für den Placeholder
        Glide.with(holder.itemView.context)
            .load(song.artworkUri)
            // Ersetze 'ic_music_note' durch ein Bild, das in deinem 'res/drawable' Ordner liegt!
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .into(holder.ivCover)

        // 2. Logik für die Tracknummer
        if (showTrackNumber) {
            holder.tvTrackNumber.visibility = View.VISIBLE
            holder.tvTrackNumber.text = String.format("%02d", song.trackNumber)
        } else {
            holder.tvTrackNumber.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(song) }
    }

    override fun getItemCount(): Int = songs.size
}