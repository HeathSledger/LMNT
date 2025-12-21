package com.example.lmnt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.*

class SongsAdapter(
    private val songs: List<Song>,
    private val showTrackNumber: Boolean = false,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>(), SectionIndexer {

    // --- SectionIndexer Logik ---
    private var sections: Array<String> = emptyArray()
    private var sectionPositions: IntArray = intArrayOf()

    init {
        setupSections()
    }

    // Berechnet die Buchstaben-Einteilung
    fun setupSections() {
        val sectionList = mutableListOf<String>()
        val positionList = mutableListOf<Int>()

        for (i in songs.indices) {
            val title = songs[i].title
            // Nimm den ersten Buchstaben oder # für Zahlen/Sonderzeichen
            val firstChar = if (title.isNotEmpty()) title[0].uppercaseChar() else '#'
            val section = if (firstChar.isLetter()) firstChar.toString() else "#"

            if (!sectionList.contains(section)) {
                sectionList.add(section)
                positionList.add(i)
            }
        }
        sections = sectionList.toTypedArray()
        sectionPositions = positionList.toIntArray()
    }

    override fun getSections(): Array<Any> = sections as Array<Any>

    override fun getPositionForSection(sectionIndex: Int): Int {
        return if (sectionIndex in sectionPositions.indices) sectionPositions[sectionIndex] else 0
    }

    override fun getSectionForPosition(position: Int): Int {
        if (position !in songs.indices) return 0
        val title = songs[position].title
        val firstChar = if (title.isNotEmpty()) title[0].uppercaseChar() else '#'
        val section = if (firstChar.isLetter()) firstChar.toString() else "#"
        return sections.indexOf(section).coerceAtLeast(0)
    }
    // --- Ende SectionIndexer ---

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTrackNumber: TextView = view.findViewById(R.id.tvTrackNumber)
        val tvTitle: TextView = view.findViewById(R.id.songTitle)
        val tvArtist: TextView = view.findViewById(R.id.songArtist)
        val ivCover: ImageView = view.findViewById(R.id.ivSongCover)
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

        Glide.with(holder.itemView.context)
            .load(song.artworkUri)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .into(holder.ivCover)

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