package com.example.audify.ui.detail

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
import com.example.audify.service.AudioForegroundService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DetailFragment : Fragment() {

    companion object {
        private const val TAG = "DetailFragment"
    }

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private var podcast: Podcast? = null
    private var service: AudioForegroundService? = null
    private var isBound = false
    private val handler = Handler(Looper.getMainLooper())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioForegroundService.LocalBinder
            service = localBinder.getService()
            isBound = true
            Log.d(TAG, "Service connected")
            setupServiceCallbacks()
            updatePlayPauseButton()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            service?.let { svc ->
                if (svc.isPlaying) {
                    val current = svc.currentPosition
                    binding.txtCurrentTime.text = formatTime(current)
                    binding.seekBar.progress = current
                    binding.seekBar.max = svc.duration
                    binding.txtTotalTime.text = formatTime(svc.duration)
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
            bindAudioService()
        }
    }

    private fun setupServiceCallbacks() {
        service?.onPreparedListener = { _ ->
            handler.post {
                updatePlayPauseButton()
                handler.post(updateSeekBar)
            }
        }
        service?.onCompletionListener = {
            handler.post {
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                binding.seekBar.progress = 0
                binding.txtCurrentTime.text = "00:00"
            }
        }
        service?.onPlayStateChanged = { playing ->
            handler.post {
                updatePlayPauseButton()
                if (playing) handler.post(updateSeekBar) else handler.removeCallbacks(updateSeekBar)
            }
        }
        service?.onErrorListener = { msg ->
            handler.post {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePlayPauseButton() {
        val playing = service?.isPlaying == true
        binding.btnPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
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
            binding.btnFavorite.isEnabled = false
            lifecycleScope.launch {
                val isFav = SupabaseService.isFavorited(userId, podcastIdStr)
                val result = if (isFav) {
                    SupabaseService.removeFavorite(userId, podcastIdStr)
                } else {
                    SupabaseService.addFavorite(
                        SupabaseService.Favorite(user_id = userId, podcast_id = podcastIdStr)
                    )
                }
                binding.btnFavorite.isEnabled = true
                if (result.isSuccess) {
                    if (isFav) {
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                    } else {
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                    }
                } else {
                    Toast.makeText(requireContext(), "No pudimos actualizar el favorito. Intenta de nuevo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnRewind.setOnClickListener { service?.seekRelative(-10000) }
        binding.btnForward.setOnClickListener { service?.seekRelative(10000) }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    service?.seekTo(progress)
                    binding.txtCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun bindAudioService() {
        val p = podcast ?: return
        val url = p.audioUrl
        if (url.isEmpty()) {
            Toast.makeText(requireContext(), "Este podcast no tiene audio disponible", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "bindAudioService: audioUrl=$url approved=${p.approved}")
        lifecycleScope.launch {
            val resolvedUrl = SupabaseService.resolveAudioUrl(url, p.approved)
            Log.d(TAG, "resolveAudioUrl result: $resolvedUrl")
            if (resolvedUrl == null) {
                Toast.makeText(requireContext(), "Este podcast está en revisión y aún no puede escucharse", Toast.LENGTH_LONG).show()
                binding.btnPlayPause.visibility = View.GONE
                binding.btnRewind.visibility = View.GONE
                binding.btnForward.visibility = View.GONE
                binding.seekBar.visibility = View.GONE
                binding.txtCurrentTime.visibility = View.GONE
                binding.txtTotalTime.visibility = View.GONE
                return@launch
            }

            val intent = Intent(requireContext(), AudioForegroundService::class.java).apply {
                action = AudioForegroundService.ACTION_PLAY
                putExtra(AudioForegroundService.EXTRA_URL, resolvedUrl)
                putExtra(AudioForegroundService.EXTRA_TITLE, p.title)
            }
            requireContext().startForegroundService(intent)
            requireContext().bindService(intent, serviceConnection, 0)
        }
    }

    private fun togglePlayPause() {
        if (isBound) {
            service?.togglePlayPause()
        }
    }

    private fun formatTime(millis: Int): String {
        val fmt = SimpleDateFormat("mm:ss", Locale.getDefault())
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis.toLong()))
    }

    override fun onResume() {
        super.onResume()
        updatePlayPauseButton()
        if (service?.isPlaying == true) handler.post(updateSeekBar)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBar)
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        service = null
        _binding = null
    }
}
