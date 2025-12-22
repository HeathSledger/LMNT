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
        // WICHTIG: Prüfe in item_top_artist.xml ob die IDs tvArtistName und tvPlayCount heißen
        val tvTitle: TextView = view.findViewById(R.id.tvArtistName)
        val tvSubTitle: TextView = view.findViewById(R.id.tvPlayCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_artist, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val stat = albumsStats[position]

        holder.tvRank.text = (position + 1).toString()

        // Titel setzen (stat.album kommt aus deinem DAO)
        holder.tvTitle.text = stat.album

        val minutes = stat.totalTime / (1000 * 60)
        holder.tvSubTitle.text = "${stat.artist} • $minutes min"

        // Fehlerquelle Suche: Wir trimmen beide Strings (entfernen Leerzeichen)
        val albumMetadata = allAlbums.find {
            it.title.trim().equals(stat.album.trim(), ignoreCase = true)
        }

        holder.ivCover.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(albumMetadata?.artworkUri) // Nutzt die Uri aus deinem Album-Modell
            .placeholder(R.drawable.ic_default_album)
            .error(R.drawable.ic_default_album)
            .centerCrop()
            .into(holder.ivCover)
    }

    override fun getItemCount() = albumsStats.size
}