package com.example.lmnt.database

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R

class TopArtistAdapter(private val artists: List<ArtistTimeStat>) :
    RecyclerView.Adapter<TopArtistAdapter.ArtistViewHolder>() {

    class ArtistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvName: TextView = view.findViewById(R.id.tvArtistName)
        val tvTime: TextView = view.findViewById(R.id.tvPlayCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        // Nutzt das neue Layout, das wir gerade erstellt haben
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_artist, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val item = artists[position]

        holder.tvRank.text = (position + 1).toString()
        holder.tvName.text = item.artist

        // Umrechnung von Millisekunden in Minuten
        val minutes = item.totalTime / (1000 * 60)
        holder.tvTime.text = "$minutes min listened"
    }

    override fun getItemCount() = artists.size
}