package com.example.audify.ui.detail

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentDetailBinding
import com.example.audify.model.Podcast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isPrepared = false
    private val handler = Handler(Looper.getMainLooper())
    private var podcast: Podcast? = null

    private val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    val current = mp.currentPosition
                    binding.txtCurrentTime.text = formatTime(current)
                    binding.seekBar.progress = current
                    binding.seekBar.max = mp.duration
                    binding.txtTotalTime.text = formatTime(mp.duration)
                    handler.postDelayed(this, 200)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val podcastId = arguments?.getInt("podcastId", -1) ?: -1
        if (podcastId == -1) {
            Toast.makeText(requireContext(), "No encontramos ese podcast", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            podcast = SupabaseService.getPodcastByIntId(podcastId)
            binding.progressBar.visibility = View.GONE

            if (podcast == null) {
                Toast.makeText(requireContext(), "No encontramos ese podcast", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return@launch
            }

            bindPodcast()
            setupClickListeners()
            initMediaPlayer()
        }
    }

    private fun bindPodcast() {
        val p = podcast ?: return
        binding.txtCoverLetter.text = p.title.firstOrNull()?.uppercase() ?: "?"
        binding.txtTitle.text = p.title
        binding.txtAuthor.text = p.author
        binding.txtCategory.text = p.category.ifEmpty { "General" }
        binding.txtDescription.text = p.description

        if (!p.coverUrl.isNullOrEmpty()) {
            binding.ivCover.load(p.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_violet)
                error(R.drawable.bg_circle_violet)
                transformations(RoundedCornersTransformation(24f))
            }
        }

        val userId = SessionManager.getUserId()
        if (userId != null) {
            lifecycleScope.launch {
                val isFav = SupabaseService.isFavorited(userId, p.supabaseId)
                binding.btnFavorite.setImageResource(
                    if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnFavorite.setOnClickListener {
            val p = podcast ?: return@setOnClickListener
            val userId = SessionManager.getUserId() ?: run {
                Toast.makeText(requireContext(), "Ingresa para guardar favoritos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val podcastIdStr = p.supabaseId
            lifecycleScope.launch {
                val isFav = SupabaseService.isFavorited(userId, podcastIdStr)
                if (isFav) {
                    SupabaseService.removeFavorite(userId, podcastIdStr)
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                } else {
                    SupabaseService.addFavorite(
                        SupabaseService.Favorite(user_id = userId, podcast_id = podcastIdStr)
                    )
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                }
            }
        }

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnRewind.setOnClickListener { seekRelative(-10000) }
        binding.btnForward.setOnClickListener { seekRelative(10000) }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer?.seekTo(progress)
                    binding.txtCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initMediaPlayer() {
        val url = podcast?.audioUrl ?: return
        if (url.isEmpty()) {
            Toast.makeText(requireContext(), "Este podcast no tiene audio disponible", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val resolvedUrl = SupabaseService.resolveAudioUrl(url)
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(resolvedUrl)
                    setOnPreparedListener { mp ->
                        this@DetailFragment.isPrepared = true
                        binding.seekBar.max = mp.duration
                        binding.txtTotalTime.text = formatTime(mp.duration)
                    }
                    setOnCompletionListener {
                        this@DetailFragment.isPlaying = false
                        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                        binding.seekBar.progress = 0
                        binding.txtCurrentTime.text = "00:00"
                    }
                    setOnErrorListener { _, _, _ ->
                        this@DetailFragment.isPrepared = false
                        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No pudimos cargar el audio. Intenta de nuevo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp == null || !isPrepared) {
            Toast.makeText(requireContext(), "Un segundito, estamos preparando el audio", Toast.LENGTH_SHORT).show()
            return
        }
        if (isPlaying) {
            mp.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            handler.removeCallbacks(updateSeekBar)
        } else {
            mp.start()
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            handler.post(updateSeekBar)
        }
        isPlaying = !isPlaying
    }

    private fun seekRelative(millis: Int) {
        mediaPlayer?.let { mp ->
            val newPos = (mp.currentPosition + millis).coerceIn(0, mp.duration)
            mp.seekTo(newPos)
            binding.txtCurrentTime.text = formatTime(newPos)
            binding.seekBar.progress = newPos
        }
    }

    private fun formatTime(millis: Int): String {
        val fmt = SimpleDateFormat("mm:ss", Locale.getDefault())
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis.toLong()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBar)
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        isPlaying = false
        _binding = null
    }
}
