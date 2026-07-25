package com.example.audify.ui.detail

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Paint
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.databinding.FragmentDetailBinding
import com.example.audify.model.Podcast
import com.example.audify.service.AudioForegroundService
import com.example.audify.viewmodel.DetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DetailFragment : Fragment() {

    companion object {
        private const val TAG = "DetailFragment"
        private const val ARG_QUEUE_PODCAST_IDS = "queuePodcastIds"
        private const val ARG_QUEUE_INDEX = "queueIndex"
    }

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()

    private var queuePodcastIds: IntArray? = null
    private var queueIndex: Int = -1
    private var service: AudioForegroundService? = null
    private var isBound = false
    private val handler = Handler(Looper.getMainLooper())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (_binding == null) return
            val localBinder = binder as AudioForegroundService.LocalBinder
            service = localBinder.getService()
            isBound = true
            Log.d(TAG, "Service connected")
            setupServiceCallbacks()
            updatePlayPauseButton()
            if (service?.isPlaying == true) {
                handler.removeCallbacks(updateSeekBar)
                handler.post(updateSeekBar)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            handler.removeCallbacks(updateSeekBar)
            if (_binding != null) {
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            val b = _binding ?: return
            service?.let { svc ->
                if (svc.isPlaying) {
                    val current = svc.currentPosition
                    b.txtCurrentTime.text = formatTime(current)
                    b.seekBar.progress = current
                    b.seekBar.max = svc.duration
                    b.txtTotalTime.text = formatTime(svc.duration)
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
        queuePodcastIds = arguments?.getIntArray(ARG_QUEUE_PODCAST_IDS)
        queueIndex = arguments?.getInt(ARG_QUEUE_INDEX, -1) ?: -1
        if (podcastId == -1) {
            Toast.makeText(requireContext(), "No encontramos ese podcast", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        setupClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadPodcast(podcastId)
    }

    private fun bindState(state: com.example.audify.viewmodel.DetailUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.layoutCover.visibility = View.INVISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
            return
        }
        binding.progressBar.visibility = View.GONE
        binding.layoutCover.visibility = View.VISIBLE
        binding.scrollContent.visibility = View.VISIBLE

        val p = state.podcast ?: return

        binding.txtCoverLetter.text = p.title.firstOrNull()?.uppercase() ?: "?"
        binding.txtTitle.text = p.title
        binding.txtAuthor.text = p.author
        binding.txtCategory.text = p.category.ifEmpty { "General" }
        binding.txtDescription.text = p.description

        val canOpenAuthor = p.userId.isNotBlank()
        binding.txtAuthor.isClickable = canOpenAuthor
        binding.txtAuthor.isFocusable = canOpenAuthor
        binding.txtAuthor.paintFlags = if (canOpenAuthor) {
            binding.txtAuthor.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            binding.txtAuthor.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        }
        binding.txtAuthor.setOnClickListener {
            if (canOpenAuthor) openAuthorProfile(p.userId)
        }

        if (!p.coverUrl.isNullOrEmpty()) {
            binding.ivCover.load(p.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_violet)
                error(R.drawable.bg_circle_violet)
                transformations(RoundedCornersTransformation(24f))
            }
        }

        binding.btnFavorite.setImageResource(
            if (state.isFavorited) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnRewind.setOnClickListener { service?.seekRelative(-10000) }
        binding.btnForward.setOnClickListener { service?.seekRelative(10000) }
        binding.btnPrevPodcast.setOnClickListener {
            if (!isQueueMode()) {
                Toast.makeText(requireContext(), "Este control funciona al reproducir una lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!playPreviousFromQueueIfNeeded()) {
                Toast.makeText(requireContext(), "Ya estás en el primer podcast de la lista", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnNextPodcast.setOnClickListener {
            if (!isQueueMode()) {
                Toast.makeText(requireContext(), "Este control funciona al reproducir una lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!playNextFromQueueIfNeeded()) {
                Toast.makeText(requireContext(), "Fin de la lista", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun openAuthorProfile(userId: String) {
        val root = view ?: return
        val bundle = Bundle().apply {
            putString("userId", userId)
            putString("authorName", viewModel.uiState.value.podcast?.author)
        }
        androidx.navigation.Navigation.findNavController(root).navigate(R.id.userProfileFragment, bundle)
    }

    private fun setupServiceCallbacks() {
        service?.onPreparedListener = { _ ->
            handler.post {
                if (_binding == null) return@post
                updatePlayPauseButton()
                handler.post(updateSeekBar)
            }
        }
        service?.onCompletionListener = {
            handler.post {
                if (_binding == null) return@post
                if (playNextFromQueueIfNeeded()) return@post
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                binding.seekBar.progress = 0
                binding.txtCurrentTime.text = "00:00"
            }
        }
        service?.onPlayStateChanged = { playing ->
            handler.post {
                if (_binding == null) return@post
                updatePlayPauseButton()
                if (playing) handler.post(updateSeekBar) else handler.removeCallbacks(updateSeekBar)
            }
        }
        service?.onErrorListener = { msg ->
            handler.post {
                if (!isAdded || _binding == null) return@post
                if (service?.isPlaying == true) return@post
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePlayPauseButton() {
        if (_binding == null) return
        val playing = service?.isPlaying == true
        binding.btnPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun bindAudioService() {
        val p = viewModel.uiState.value.podcast ?: return
        val url = p.audioUrl
        if (url.isEmpty()) {
            if (isQueueMode() && playNextFromQueueIfNeeded()) return
            Toast.makeText(requireContext(), "Este podcast no tiene audio disponible", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "bindAudioService: audioUrl=$url approved=${p.approved}")
        viewModel.resolveAudio()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null || !isAdded) return@collect
                    if (!state.audioResolved) return@collect
                    val resolvedUrl = state.resolvedAudioUrl
                    if (resolvedUrl == null) {
                        if (isQueueMode() && playNextFromQueueIfNeeded()) return@collect
                        Toast.makeText(requireContext(), "Este podcast está en revisión y aún no puede escucharse", Toast.LENGTH_LONG).show()
                        binding.btnPlayPause.visibility = View.GONE
                        binding.btnRewind.visibility = View.GONE
                        binding.btnForward.visibility = View.GONE
                        binding.seekBar.visibility = View.GONE
                        binding.txtCurrentTime.visibility = View.GONE
                        binding.txtTotalTime.visibility = View.GONE
                        return@collect
                    }

                    val intent = Intent(requireContext(), AudioForegroundService::class.java).apply {
                        action = AudioForegroundService.ACTION_PLAY
                        putExtra(AudioForegroundService.EXTRA_URL, resolvedUrl)
                        putExtra(AudioForegroundService.EXTRA_TITLE, p.title)
                        putExtra(AudioForegroundService.EXTRA_PODCAST_ID, p.id)
                    }
                    requireContext().startForegroundService(intent)
                    requireContext().bindService(intent, serviceConnection, 0)
                }
        }
    }

    private fun togglePlayPause() {
        val svc = service
        if (!isBound || svc == null || !svc.hasActivePlaybackSession) {
            bindAudioService()
            return
        }
        svc.togglePlayPause()
    }

    private fun isQueueMode(): Boolean {
        val ids = queuePodcastIds
        return ids != null && ids.size > 1 && queueIndex >= 0
    }

    private fun playPreviousFromQueueIfNeeded(): Boolean {
        val ids = queuePodcastIds ?: return false
        if (ids.size <= 1 || queueIndex <= 0) return false
        queueIndex -= 1
        loadPodcastFromQueue(ids[queueIndex])
        return true
    }

    private fun playNextFromQueueIfNeeded(): Boolean {
        val ids = queuePodcastIds ?: return false
        if (ids.size <= 1 || queueIndex < 0) return false
        val nextIndex = queueIndex + 1
        if (nextIndex >= ids.size) return false
        queueIndex = nextIndex
        loadPodcastFromQueue(ids[nextIndex])
        return true
    }

    private fun loadPodcastFromQueue(podcastId: Int) {
        viewModel.loadPodcast(podcastId)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null || !isAdded) return@collect
                    if (state.isLoading) return@collect
                    if (state.podcast == null) {
                        Toast.makeText(requireContext(), "No pudimos cargar el siguiente podcast", Toast.LENGTH_SHORT).show()
                        return@collect
                    }
                    bindAudioService()
                }
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
        if (service?.isPlaying == true) {
            handler.removeCallbacks(updateSeekBar)
            handler.post(updateSeekBar)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBar)
        service?.onPreparedListener = null
        service?.onCompletionListener = null
        service?.onPlayStateChanged = null
        service?.onErrorListener = null
        if (isBound) {
            try {
                requireContext().unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        service = null
        _binding = null
    }
}
