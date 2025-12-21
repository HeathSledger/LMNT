package com.example.lmnt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lmnt.Song
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // Die "Master-Listen" (Originale vom MediaStore)
    private var originalSongs = listOf<Song>()
    private var originalAlbums = listOf<Album>()
    private var originalArtists = listOf<Artist>()

    // Die LiveData, die die Fragmente beobachten (gefiltert/sortiert)
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> = _albums

    private val _artists = MutableLiveData<List<Artist>>()
    val artists: LiveData<List<Artist>> = _artists

    // Daten setzen und Backups speichern
    fun setSongs(list: List<Song>) {
        originalSongs = list
        _songs.postValue(list)
    }
    fun setAlbums(list: List<Album>) {
        originalAlbums = list
        _albums.postValue(list)
    }
    fun setArtists(list: List<Artist>) {
        originalArtists = list
        _artists.postValue(list)
    }
    // --- SORTIERUNG ---
    fun sortSongs(ascending: Boolean) {
        val currentList = _songs.value ?: originalSongs
        val sorted = if (ascending) {
            currentList.sortedBy { it.title.lowercase() }
        } else {
            currentList.sortedByDescending { it.title.lowercase() }
        }
        _songs.value = sorted
    }
    // --- GLOBALE SUCHE ---
    fun filterAll(query: String) {
        val q = query.lowercase()

        if (q.isEmpty()) {
            _songs.value = originalSongs
            _albums.value = originalAlbums
            _artists.value = originalArtists
        } else {
            _songs.value = originalSongs.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            _albums.value = originalAlbums.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            _artists.value = originalArtists.filter { it.name.lowercase().contains(q) }
        }
    }
}