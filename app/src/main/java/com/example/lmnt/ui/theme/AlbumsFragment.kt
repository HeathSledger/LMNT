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
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_albums, container, false)

        recyclerView = view.findViewById(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val albums = loadAlbums()

        albumAdapter = AlbumAdapter(albums) { album ->
            openAlbumDetails(album)
        }
        recyclerView.adapter = albumAdapter

        return view
    }

    private fun openAlbumDetails(album: Album) {
        // Wir übergeben ID und Artwork-Pfad an das neue Fragment
        val detailFragment = AlbumDetailFragment.newInstance(album.id, album.artworkUri)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadAlbums(): List<Album> {
        val albumList = mutableListOf<Album>()
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        // Sortierung nach Album-Name
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        context?.contentResolver?.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songsCountCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumName = cursor.getString(albumCol)
                val artist = cursor.getString(artistCol)
                val songCount = cursor.getInt(songsCountCol) // Das ist ein Int

                // Erstellt die Uri für das Cover-Bild
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                ).toString()

                albumList.add(Album(id, albumName, artist, artworkUri, songCount))
            }
        }
        return albumList
    }
}