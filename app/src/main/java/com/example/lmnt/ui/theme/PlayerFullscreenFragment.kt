package com.example.lmnt

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayerFullscreenFragment : BottomSheetDialogFragment() {

    private var controller: Player? = null
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseBtn: ImageButton
    private lateinit var currentTimeTv: TextView
    private lateinit var totalTimeTv: TextView
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnRepeat: ImageButton

    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressAction = object : Runnable {
        override fun run() {
            controller?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition.toInt()
                    currentTimeTv.text = formatTime(it.currentPosition)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_player_fullscreen, container, false)
        controller = (activity as? MainActivity)?.mediaController

        // Views initialisieren
        val titleTv = view.findViewById<TextView>(R.id.playerTitle)
        val artistTv = view.findViewById<TextView>(R.id.playerArtist)
        val albumArtIv = view.findViewById<ImageView>(R.id.playerAlbumArt)
        playPauseBtn = view.findViewById<ImageButton>(R.id.btnPlayPause)
        seekBar = view.findViewById(R.id.playerSeekBar)
        currentTimeTv = view.findViewById(R.id.tvCurrentTime)
        totalTimeTv = view.findViewById(R.id.tvTotalTime)

        val btnNext = view.findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrev)
        btnShuffle = view.findViewById(R.id.btnShuffle)
        btnRepeat = view.findViewById(R.id.btnRepeat)

        fun updateUI() {
            val metadata = controller?.currentMediaItem?.mediaMetadata
            titleTv.text = metadata?.title ?: "Unbekannt"
            artistTv.text = metadata?.artist ?: "Unbekannt"
            albumArtIv.load(metadata?.artworkUri) {
                error(R.drawable.ic_music_note)
            }

            playPauseBtn.setImageResource(
                if (controller?.isPlaying == true) R.drawable.player_pause
                else R.drawable.player_play
            )

            // Shuffle/Repeat Status Buttons einfärben
            updateToggleButtons()

            controller?.duration?.let { duration ->
                if (duration > 0) {
                    seekBar.max = duration.toInt()
                    totalTimeTv.text = formatTime(duration)
                }
            }
        }

        // --- KLICK LISTENER ---

        playPauseBtn.setOnClickListener {
            if (controller?.isPlaying == true) controller?.pause() else controller?.play()
        }

        btnNext.setOnClickListener {
            if (controller?.hasNextMediaItem() == true) controller?.seekToNext()
            else Toast.makeText(context, "Ende der Liste", Toast.LENGTH_SHORT).show()
        }

        btnPrev.setOnClickListener {
            if (controller?.hasPreviousMediaItem() == true) controller?.seekToPrevious()
        }

        btnShuffle.setOnClickListener {
            controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false)
            updateToggleButtons()
        }

        btnRepeat.setOnClickListener {
            controller?.repeatMode = when (controller?.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            updateToggleButtons()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    controller?.seekTo(progress.toLong())
                    currentTimeTv.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        updateUI()

        controller?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { updateUI() }
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateUI() }
            override fun onPlaybackStateChanged(state: Int) { updateUI() }
        })

        handler.post(updateProgressAction)
        return view
    }

    // Hilfsfunktion um die Farben der Shuffle/Repeat Buttons zu aktualisieren
    private fun updateToggleButtons() {
        val activeColor = ColorStateList.valueOf(Color.GREEN) // Oder deine Wunschfarbe
        val inactiveColor = ColorStateList.valueOf(Color.GRAY)

        // --- SHUFFLE ---
        val isShuffleOn = controller?.shuffleModeEnabled == true
        btnShuffle.imageTintList = if (isShuffleOn) activeColor else inactiveColor
        // Falls du ein spezielles Icon für "Shuffle On" hast, hier wechseln:
        // btnShuffle.setImageResource(if (isShuffleOn) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)

        // --- REPEAT (3-Wege-Wechsel) ---
        when (controller?.repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                btnRepeat.setImageResource(R.drawable.repeat_off) // Dein Icon für Repeat Aus
                btnRepeat.imageTintList = inactiveColor
            }
            Player.REPEAT_MODE_ALL -> {
                btnRepeat.setImageResource(R.drawable.repeat) // Dein Icon für Repeat Playlist
                btnRepeat.imageTintList = activeColor
            }
            Player.REPEAT_MODE_ONE -> {
                btnRepeat.setImageResource(R.drawable.repeat_once) // Dein Icon für Repeat 1 Song
                btnRepeat.imageTintList = activeColor
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateProgressAction)
    }
}