package com.isetr.cupcake.ui.intro

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.isetr.cupcake.R

class IntroVideoFragment : Fragment() {

    private val args: IntroVideoFragmentArgs by navArgs()
    private var videoView: VideoView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_intro_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoView = view.findViewById(R.id.videoView)

        // Configuration du chemin de la vidéo (res/raw/intro_video.mp4)
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.intro_video}"
        val uri = Uri.parse(videoPath)

        videoView?.apply {
            setVideoURI(uri)
            
            // Lancer la vidéo dès qu'elle est prête
            setOnPreparedListener { mp ->
                mp.isLooping = false
                start()
            }

            // Naviguer automatiquement à la fin de la vidéo
            setOnCompletionListener {
                navigateToWelcome()
            }

            // Gestion d'erreur (si la vidéo ne peut pas être lue, on navigue quand même)
            setOnErrorListener { _, _, _ ->
                navigateToWelcome()
                true
            }
        }
    }

    private fun navigateToWelcome() {
        if (isAdded) {
            val action = IntroVideoFragmentDirections.actionIntroVideoFragmentToWelcomeFragment(
                nom = args.nom,
                prenom = args.prenom
            )
            findNavController().navigate(action)
        }
    }

    override fun onPause() {
        super.onPause()
        videoView?.pause()
    }

    override fun onResume() {
        super.onResume()
        videoView?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoView?.stopPlayback()
        videoView = null
    }
}
