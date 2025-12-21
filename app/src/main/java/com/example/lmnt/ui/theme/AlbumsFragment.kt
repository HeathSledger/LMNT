package com.example.lmnt.ui

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.model.Album

class AlbumsFragment : Fragment(R.layout.fragment_albums) {

    private lateinit var recyclerView: RecyclerView
    private var allAlbums = listOf<Album>()
    private val displayedAlbums = mutableListOf<Album>()
    private lateinit var albumAdapter: AlbumAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        if (allAlbums.isEmpty()) {
            allAlbums = loadAlbumsFromStorage()
        }

        displayedAlbums.clear()
        displayedAlbums.addAll(allAlbums)

        albumAdapter = AlbumAdapter(displayedAlbums) { album ->
            openAlbumDetails(album)
        }
        recyclerView.adapter = albumAdapter
    }

    private fun openAlbumDetails(album: Album) {
        activity?.findViewById<View>(R.id.fragment_container)?.visibility = View.VISIBLE

        val detailFragment = AlbumDetailFragment.newInstance(album.id, album.artworkUri)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    // DIESE FUNKTION FEHLTE IM CODE-SNIPPET UND VERURSACHT DEN FEHLER IN DER MAINACTIVITY
    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase()
        val filtered = if (lowerCaseQuery.isEmpty()) {
            allAlbums
        } else {
            allAlbums.filter {
                it.title.lowercase().contains(lowerCaseQuery) ||
                        it.artist.lowercase().contains(lowerCaseQuery)
            }
        }
        displayedAlbums.clear()
        displayedAlbums.addAll(filtered)

        if (::albumAdapter.isInitialized) {
            albumAdapter.notifyDataSetChanged()
        }
    }

    private fun loadAlbumsFromStorage(): List<Album> {
        val albumList = mutableListOf<Album>()
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        context?.contentResolver?.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id
                ).toString()

                albumList.add(Album(
                    id = id,
                    title = cursor.getString(albumCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    artworkUri = artworkUri,
                    songCount = cursor.getInt(countCol)
                ))
            }
        }
        return albumList
    }
    fun changeGridColumns(count: Int) {
        // ID korrigiert von albumsRecyclerView zu rvAlbums
        val recyclerView = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAlbums)

        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.GridLayoutManager

        if (layoutManager != null) {
            layoutManager.spanCount = count
            // Bei GridLayoutManager ist es oft besser, dem Adapter zu sagen,
            // dass sich das Layout geändert hat, um die Item-Breiten neu zu berechnen
            recyclerView.adapter?.notifyDataSetChanged()
            layoutManager.requestLayout()
        }
    }
}