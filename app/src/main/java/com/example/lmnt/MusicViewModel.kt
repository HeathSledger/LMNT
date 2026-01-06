package com.example.lmnt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lmnt.Song
import com.example.lmnt.database.AppDatabase
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch // WICHTIG: Dieser Import hat gefehlt
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // Datenbank-Instanz
    private val db = AppDatabase.getDatabase(application)

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

    // LiveData für die Favoriten-Playlist
    private val _favoriteSongs = MutableLiveData<List<Song>>()
    val favoriteSongs: LiveData<List<Song>> = _favoriteSongs

    // Hilfsfunktion für das SongsFragment
    suspend fun getFavoriteIds(): List<Long> {
        return withContext(Dispatchers.IO) {
            db.historyDao().getFavoriteSongIds()
        }
    }

    fun refreshFavorites() {
        // Hier wird 'launch' jetzt durch den Import oben erkannt
        viewModelScope.launch {
            val favIds = withContext(Dispatchers.IO) {
                db.historyDao().getFavoriteSongIds()
            }
            // Wir filtern die originalen Songs basierend auf den IDs aus der DB
            val favList = originalSongs.filter { it.id in favIds }
            _favoriteSongs.postValue(favList)
        }
    }

    // Daten setzen und Backups speichern
    fun setSongs(list: List<Song>) {
        originalSongs = list
        _songs.postValue(list)
        // Nach dem ersten Laden auch die Favoriten einmal aktualisieren
        refreshFavorites()
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