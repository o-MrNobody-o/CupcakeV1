package com.isetr.cupcake.ui.intro

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.isetr.cupcake.R

class IntroVideoFragment : Fragment() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private val args: IntroVideoFragmentArgs by navArgs()
    private val handler = Handler(Looper.getMainLooper())
    private var skipRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_intro_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.hide()


        playerView = view.findViewById(R.id.player_view)

        // Initialize ExoPlayer
        initializePlayer()

        // Optional: Allow skip after 3 seconds
        setupSkipOption()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player

        // Load video from raw resources
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.intro_video}")
        val mediaItem = MediaItem.fromUri(videoUri)
        player?.setMediaItem(mediaItem)

        // Prepare and play
        player?.prepare()
        player?.playWhenReady = true

        // Listen for playback state changes
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    navigateToWelcome()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Handle error, for now just navigate
                navigateToWelcome()
            }
        })
    }

    private fun setupSkipOption() {
        // Allow skip after 3 seconds
        skipRunnable = Runnable {
            playerView.setOnClickListener {
                navigateToWelcome()
            }
        }
        handler.postDelayed(skipRunnable!!, 3000) // 3 seconds
    }

    private fun navigateToWelcome() {
        // Cancel skip runnable if not used
        skipRunnable?.let { handler.removeCallbacks(it) }

        // Navigate to WelcomeFragment with arguments
        val action = IntroVideoFragmentDirections.actionIntroVideoFragmentToWelcomeFragment(
            nom = args.nom,
            prenom = args.prenom
        )
        findNavController().navigate(action)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}