package com.example.lmnt.ui.theme

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.model.Album
import com.example.lmnt.ui.AlbumDetailFragment

class AlbumsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    // Wir nutzen nur EINE Liste und EINEN Adapter für die Anzeige
    private var allAlbums = listOf<Album>()
    private val displayedAlbums = mutableListOf<Album>()
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_albums, container, false)

        recyclerView = view.findViewById(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // 1. Daten laden
        allAlbums = loadAlbumsFromStorage()
        displayedAlbums.clear()
        displayedAlbums.addAll(allAlbums)

        // 2. Adapter initialisieren (WICHTIG: Er nutzt displayedAlbums)
        albumAdapter = AlbumAdapter(displayedAlbums) { album ->
            openAlbumDetails(album)
        }
        recyclerView.adapter = albumAdapter

        return view
    }

    private fun openAlbumDetails(album: Album) {
        val detailFragment = AlbumDetailFragment.newInstance(album.id, album.artworkUri)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadAlbumsFromStorage(): List<Album> {
        val albumList = mutableListOf<Album>()
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        context?.contentResolver?.query(uri, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songsCountCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id
                ).toString()

                albumList.add(Album(
                    id,
                    cursor.getString(albumCol) ?: "Unbekannt",
                    cursor.getString(artistCol) ?: "Unbekannter Künstler",
                    artworkUri,
                    cursor.getInt(songsCountCol)
                ))
            }
        }
        return albumList
    }

    // DIE SUCHFUNKTION (Wird von MainActivity aufgerufen)
    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase()
        val filtered = if (lowerCaseQuery.isEmpty()) {
            allAlbums
        } else {
            allAlbums.filter {
                // Achte darauf, ob dein Model "title" oder "album" heißt!
                it.title.lowercase().contains(lowerCaseQuery) ||
                        it.artist.lowercase().contains(lowerCaseQuery)
            }
        }
        displayedAlbums.clear()
        displayedAlbums.addAll(filtered)
        // Sicherstellen, dass der Adapter existiert, bevor wir ihn benachrichtigen
        if (::albumAdapter.isInitialized) {
            albumAdapter.notifyDataSetChanged()
        }
    }
}