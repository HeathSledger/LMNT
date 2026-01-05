package com.example.lmnt

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist

object MusicLoader {

    private val albumArtUri = "content://media/external/audio/albumart".toUri()
    private val baseAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    fun loadAllSongs(contentResolver: ContentResolver): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION
        )
        contentResolver.query(
            baseAudioUri,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)

                // Content-URI für den Song erstellen
                val contentUri = ContentUris.withAppendedId(baseAudioUri, id).toString()
                val artworkUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()

                songList.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unbekannt",
                        artist = cursor.getString(artistCol) ?: "Unbekannt",
                        album = cursor.getString(albumCol) ?: "Unbekannt",
                        uri = contentUri, // <--- Jetzt eine sichere URI
                        artworkUri = artworkUri,
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        discNumber = 1,
                        duration = cursor.getInt(durationCol)
                    )
                )
            }
        }
        return songList
    }

        fun loadRecentlyAdded(contentResolver: ContentResolver, limit: Int = 50): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        // NUR die Sortierung, OHNE das Wort "LIMIT"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            var count = 0
            // Wir prüfen zusätzlich zum cursor auch den "count"
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)

                val contentUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                val artworkUri = android.content.ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songList.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unbekannt",
                        artist = cursor.getString(artistCol) ?: "Unbekannt",
                        album = cursor.getString(albumCol) ?: "Unbekannt",
                        uri = contentUri,
                        artworkUri = artworkUri,
                        trackNumber = 0,
                        discNumber = 1,
                        duration = cursor.getInt(durationCol)
                    )
                )
                count++ // Zähler erhöhen
            }
        }
        return songList
    }

    fun loadSongsForAlbum(contentResolver: ContentResolver, albumId: Long): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        contentResolver.query(baseAudioUri, projection, selection, selectionArgs, "${MediaStore.Audio.Media.TRACK} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(baseAudioUri, id).toString()
                val artworkUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()

                songList.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unbekannt",
                        artist = cursor.getString(artistCol) ?: "Unbekannt",
                        album = cursor.getString(albumCol) ?: "Unbekannt",
                        uri = contentUri,
                        artworkUri = artworkUri,
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        discNumber = if (cursor.getInt(trackCol) >= 1000) cursor.getInt(trackCol) / 1000 else 1,
                        duration = cursor.getInt(durationCol)
                    )
                )
            }
        }
        return songList
    }

    fun loadSongsForArtistName(contentResolver: ContentResolver, artistName: String): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION
        )
        contentResolver.query(
            baseAudioUri,
            projection,
            "${MediaStore.Audio.Media.ARTIST} = ?",
            arrayOf(artistName),
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(baseAudioUri, id).toString()
                val artworkUri = ContentUris.withAppendedId(albumArtUri, cursor.getLong(albumIdCol)).toString()

                songList.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unbekannt",
                        artist = cursor.getString(artistCol) ?: "Unbekannt",
                        album = cursor.getString(albumCol) ?: "Unbekannt",
                        uri = contentUri,
                        artworkUri = artworkUri,
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        discNumber = 1,
                        duration = cursor.getInt(durationCol)
                    )
                )
            }
        }
        return songList
    }

    // Die anderen Funktionen (loadArtists, loadAlbums, loadAlbenForArtist)
    // bleiben gleich, da diese keine Song-URIs benötigen.

    fun loadArtists(contentResolver: ContentResolver): List<Artist> {
        val artistList = mutableListOf<Artist>()
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )
        contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projection, null, null, "${MediaStore.Audio.Artists.ARTIST} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val tracksCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            while (cursor.moveToNext()) {
                artistList.add(Artist(cursor.getLong(idCol), cursor.getString(artistCol) ?: "Unbekannt", cursor.getInt(albumsCol), cursor.getInt(tracksCol)))
            }
        }
        return artistList
    }

    fun loadAlbums(contentResolver: ContentResolver): List<Album> {
        val albumList = mutableListOf<Album>()
        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.NUMBER_OF_SONGS)
        contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(albumArtUri, id).toString()
                albumList.add(Album(id, cursor.getString(albumCol) ?: "Unbekannt", cursor.getString(artistCol) ?: "Unbekannt", artworkUri, cursor.getInt(countCol)))
            }
        }
        return albumList
    }

    fun loadAlbenForArtist(contentResolver: ContentResolver, artistName: String): List<Album> {
        val albumList = mutableListOf<Album>()
        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.NUMBER_OF_SONGS)
        contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Albums.ARTIST} = ?", arrayOf(artistName), "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(albumArtUri, id).toString()
                albumList.add(Album(id = id, title = cursor.getString(albumCol) ?: "Unbekannt", artist = cursor.getString(artistCol) ?: "Unbekannt", artworkUri = artworkUri, songCount = cursor.getInt(countCol)))
            }
        }
        return albumList
    }
}