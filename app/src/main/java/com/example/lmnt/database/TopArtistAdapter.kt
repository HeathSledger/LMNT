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

class TopArtistAdapter(
    private val artists: List<ArtistTimeStat>,
    private val allAlbums: List<Album> // Diese Zeile muss hier stehen!
) : RecyclerView.Adapter<TopArtistAdapter.ArtistViewHolder>() {

    class ArtistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val tvArtistName: TextView = view.findViewById(R.id.tvArtistName)
        val tvPlayCount: TextView = view.findViewById(R.id.tvPlayCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_artist, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artistData = artists[position]
        holder.tvRank.text = (position + 1).toString()
        holder.tvArtistName.text = artistData.artist

        val minutes = artistData.totalTime / (1000 * 60)
        holder.tvPlayCount.text = "$minutes Minutes"

        // Bild-Logik: Wir suchen ein Album dieses Künstlers für das Vorschaubild
        val artistAlbum = allAlbums.find { it.artist == artistData.artist }

        Glide.with(holder.itemView.context)
            .load(artistAlbum?.artworkUri)
            .placeholder(R.drawable.ic_default_album)
            .error(R.drawable.ic_default_album)
            .circleCrop() // Künstler sehen in Kreisen oft besser aus
            .into(holder.ivCover)
        holder.ivCover.visibility = View.GONE
    }

    override fun getItemCount() = artists.size
}