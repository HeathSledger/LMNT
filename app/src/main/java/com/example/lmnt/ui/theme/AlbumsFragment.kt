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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.model.Album
import com.example.lmnt.Song
import kotlin.collections.isNotEmpty
import kotlin.collections.map

class AlbumsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // WICHTIG: Prüfe fragment_albums.xml, ob dort das TextView "Albums" gelöscht wurde!
        val view = inflater.inflate(R.layout.fragment_albums, container, false)

        recyclerView = view.findViewById(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val albums = loadAlbums()

        albumAdapter = AlbumAdapter(albums) { album ->
            playAlbum(album)
        }
        recyclerView.adapter = albumAdapter

        return view
    }

    private fun playAlbum(album: Album) {
        val songs = loadSongsForAlbum(album.id)
        if (songs.isNotEmpty()) {
            val mediaItems = songs.map { song ->
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(Uri.parse(song.uri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(Uri.parse(song.artworkUri))
                            .build()
                    )
                    .build()
            }

            // Zugriff auf den Controller der MainActivity
            (activity as? MainActivity)?.mediaController?.let { controller ->
                controller.setMediaItems(mediaItems)
                controller.prepare()
                controller.play()
            }
        }
    }

    private fun loadSongsForAlbum(albumId: Long): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        context?.contentResolver?.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                songList.add(Song(
                    id = id,
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    uri = cursor.getString(dataCol),
                    artworkUri = albumArtUri
                ))
            }
        }
        return songList
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

        context?.contentResolver?.query(uri, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
                albumList.add(Album(id, cursor.getString(albumCol), cursor.getString(artistCol), albumArtUri.toString(), cursor.getInt(songsCol)))
            }
        }
        return albumList
    }
}