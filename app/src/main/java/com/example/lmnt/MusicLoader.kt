package com.example.lmnt

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist

object MusicLoader {

    // --- BASIS LADEN ---

    fun loadAllSongs(contentResolver: ContentResolver): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION
        )
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(albumIdCol)
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    uri = cursor.getString(dataCol) ?: "",
                    artworkUri = artworkUri,
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    discNumber = 1,
                    duration = cursor.getInt(durationCol)
                ))
            }
        }
        return songList
    }

    fun loadAlbums(contentResolver: ContentResolver): List<Album> {
        val albumList = mutableListOf<Album>()
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )
        contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id).toString()
                albumList.add(Album(id, cursor.getString(albumCol) ?: "Unbekannt", cursor.getString(artistCol) ?: "Unbekannt", artworkUri, cursor.getInt(countCol)))
            }
        }
        return albumList
    }

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

    // --- SPEZIALFILTER ---

    fun loadSongsForAlbum(contentResolver: ContentResolver, albumId: Long): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TRACK,MediaStore.Audio.Media.DURATION)
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(albumId.toString()), "${MediaStore.Audio.Media.TRACK} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    uri = cursor.getString(dataCol) ?: "",
                    artworkUri = artworkUri,
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    discNumber = 1,
                    duration = cursor.getInt(durationCol)
                ))
            }
        }
        return songList
    }

    fun loadAlbenForArtist(contentResolver: ContentResolver, artistName: String): List<Album> {
        val albumList = mutableListOf<Album>()
        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.NUMBER_OF_SONGS)
        contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Albums.ARTIST} = ?", arrayOf(artistName), "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id).toString()
                albumList.add(Album(id, cursor.getString(albumCol) ?: "Unbekannt", artistName, artworkUri, cursor.getInt(countCol)))
            }
        }
        return albumList
    }

    fun loadSongsForArtistName(contentResolver: ContentResolver, artistName: String): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.DURATION)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Media.ARTIST} = ?", arrayOf(artistName), "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albumIdCol)).toString()
                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    uri = cursor.getString(dataCol) ?: "",
                    artworkUri = artworkUri,
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    discNumber = 1,
                    duration = cursor.getInt(durationCol)
                ))
            }
        }
        return songList
    }
}