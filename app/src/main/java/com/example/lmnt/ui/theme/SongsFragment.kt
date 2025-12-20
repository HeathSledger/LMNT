package com.example.lmnt.ui.theme

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.Song
import com.example.lmnt.SongsAdapter
import com.example.lmnt.MainActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class SongsFragment : Fragment() {

    private val songs = mutableListOf<Song>()
    private lateinit var adapter: SongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_songs, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongsAdapter(songs) { song ->
            playSong(song)
        }
        recyclerView.adapter = adapter

        checkPermissionsAndLoad()

        return view
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            requestPermissions(arrayOf(permission), 101)
        }
    }

    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)
                val albumId = it.getLong(albumIdCol)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                // Wir bauen die Artwork-URI direkt hier zusammen
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                // Hier ist wichtig: Dein Song-Objekt muss nun 5 Parameter haben
                songs.add(Song(id, title, artist, uri, artworkUri))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun playSong(selectedSong: Song) {
        val mainActivity = activity as? MainActivity
        val controller = mainActivity?.mediaController ?: return

        // Wir wandeln alle Songs in MediaItems um
        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                // Hier hattest du Uri.parse bereits korrekt genutzt
                .setArtworkUri(Uri.parse(song.artworkUri))
                .build()

            MediaItem.Builder()
                .setMediaId(song.uri) // MediaId darf ein String sein
                // FIX: Hier muss Uri.parse() drumherum, damit aus dem String eine Uri wird
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(metadata)
                .build()
        }

        // Index des gewählten Songs finden
        val startIndex = songs.indexOf(selectedSong)

        // Liste an den Player übergeben
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }
}