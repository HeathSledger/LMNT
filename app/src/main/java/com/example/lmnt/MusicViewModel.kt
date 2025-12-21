package com.example.lmnt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lmnt.Song
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // LiveData hält die Daten bereit. Fragmente können diese "beobachten".
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> = _albums

    private val _artists = MutableLiveData<List<Artist>>()
    val artists: LiveData<List<Artist>> = _artists

    // Funktionen zum Setzen der Daten (wird von der Activity aufgerufen)
    fun setSongs(songList: List<Song>) { _songs.postValue(songList) }
    fun setAlbums(albumList: List<Album>) { _albums.postValue(albumList) }
    fun setArtists(artistList: List<Artist>) { _artists.postValue(artistList) }
}