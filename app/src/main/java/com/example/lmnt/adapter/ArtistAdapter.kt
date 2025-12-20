package com.example.lmnt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.model.Artist

class ArtistAdapter(
    private val artists: List<Artist>,
    private val onClick: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    class ArtistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvArtistName)
        val tvDetails: TextView = view.findViewById(R.id.tvArtistDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artist, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artists[position]

        holder.tvName.text = artist.name

        // Formatiert den Text zu "X Alben • Y Songs"
        val detailText = "${artist.albumCount} Alben • ${artist.trackCount} Songs"
        holder.tvDetails.text = detailText

        holder.itemView.setOnClickListener { onClick(artist) }
    }

    override fun getItemCount(): Int = artists.size
}