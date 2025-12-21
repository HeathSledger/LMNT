package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.model.Album
import com.example.lmnt.viewmodel.MusicViewModel

class AlbumsFragment : Fragment(R.layout.fragment_albums) {

    private lateinit var recyclerView: RecyclerView
    // allAlbums brauchen wir hier nicht mehr unbedingt als lokales Backup,
    // da das ViewModel die Master-Liste hält.
    private val displayedAlbums = mutableListOf<Album>()
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var musicViewModel: MusicViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        albumAdapter = AlbumAdapter(displayedAlbums) { album ->
            openAlbumDetails(album)
        }
        recyclerView.adapter = albumAdapter

        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        // Das ist jetzt das Herzstück:
        musicViewModel.albums.observe(viewLifecycleOwner) { albumList ->
            // Jedes Mal, wenn im ViewModel gesucht oder geladen wird,
            // kommen hier die passenden Daten an.
            updateDisplayedAlbums(albumList)
        }
    }

    private fun updateDisplayedAlbums(newList: List<Album>) {
        displayedAlbums.clear()
        displayedAlbums.addAll(newList)
        if (::albumAdapter.isInitialized) {
            albumAdapter.notifyDataSetChanged()
        }
    }

    // Die Funktion filter() kann hier weg, da die MainActivity
    // musicViewModel.filterAll() nutzt!

    fun changeGridColumns(count: Int) {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
        if (layoutManager != null) {
            layoutManager.spanCount = count
            // Wichtig: requestLayout() reicht meistens, notify ist hier oft optional
            layoutManager.requestLayout()
        }
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
}