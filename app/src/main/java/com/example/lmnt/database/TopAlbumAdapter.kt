package com.example.lmnt.database

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lmnt.R
import com.example.lmnt.model.Album

class TopAlbumAdapter(
    private val albumsStats: List<AlbumTimeStat>,
    private val allAlbums: List<Album>
) : RecyclerView.Adapter<TopAlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivCover: ImageView = view.findViewById(R.id.ivCover)

        // ACHTUNG: Hier war eventuell tvArtistName für das Album zugewiesen
        val tvAlbumTitle: TextView = view.findViewById(R.id.tvArtistName)
        val tvDetailInfo: TextView = view.findViewById(R.id.tvPlayCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        // Wir nutzen das existierende Item-Layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_artist, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val stat = albumsStats[position]

        holder.tvRank.text = (position + 1).toString()

        // 1. TITEL SETZEN
        // Da in deiner AlbumTimeStat das Feld 'album' heißt:
        holder.tvAlbumTitle.text = stat.album

        // 2. UNTERZEILE SETZEN (Interpret & Zeit)
        val minutes = stat.totalTime / (1000 * 60)
        holder.tvDetailInfo.text = "${stat.artist} • $minutes min"

        // 3. COVER FINDEN
        // Wir suchen in der Library nach dem Albumnamen
        val albumMetadata = allAlbums.find { it.title.equals(stat.album, ignoreCase = true) }

        holder.ivCover.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(albumMetadata?.artworkUri)
            .placeholder(R.drawable.ic_default_album)
            .error(R.drawable.ic_default_album)
            .centerCrop()
            .into(holder.ivCover)
    }

    override fun getItemCount() = albumsStats.size
}