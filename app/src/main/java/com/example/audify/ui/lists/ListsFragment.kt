package com.example.audify.ui.lists

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentListsBinding
import com.example.audify.model.Playlist
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PlaylistAdapter
import com.example.audify.ui.adapter.PodcastAdapter
import com.example.audify.viewmodel.ListsViewModel
import kotlinx.coroutines.launch

class ListsFragment : Fragment() {

    companion object {
        const val ARG_QUEUE_PODCAST_IDS = "queuePodcastIds"
        const val ARG_QUEUE_INDEX = "queueIndex"
    }

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllPodcasts.layoutManager = LinearLayoutManager(requireContext())

        setupToolbar()
        setupCreateList()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadPlaylists()
        viewModel.loadAllPodcasts()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPlaylists()
    }

    private fun setupToolbar() {
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
    }

    private fun bindState(state: com.example.audify.viewmodel.ListsUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.layoutHeader.visibility = View.INVISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.layoutHeader.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.VISIBLE
        }

        binding.txtTotalLists.text = state.playlists.size.toString()
        binding.rvPlaylists.adapter = PlaylistAdapter(
            state.playlists,
            ::showPlaylistDetail,
            ::confirmDeletePlaylist
        )

        binding.rvAllPodcasts.adapter = PodcastAdapter(
            state.allPodcasts,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile
        )
    }

    private fun setupCreateList() {
        binding.btnCreateList.setOnClickListener {
            if (!SessionManager.isLoggedIn()) {
                Toast.makeText(requireContext(), "Ingresa para crear listas", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                return@setOnClickListener
            }
            showCreateListDialog()
        }
    }

    private fun showCreateListDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Nombre de la lista"
            setTextColor(0xFF1E1B4B.toInt())
            setHintTextColor(0xFFA78BFA.toInt())
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Crear nueva lista")
            .setMessage("Escribe un nombre para tu lista de podcasts")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(
                        name,
                        onSuccess = { Toast.makeText(requireContext(), "¡Lista \"$name\" creada!", Toast.LENGTH_SHORT).show() },
                        onError = { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    Toast.makeText(requireContext(), "¿Cómo se llama tu lista?", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPlaylistDetail(playlist: Playlist) {
        viewModel.getPlaylistPodcasts(playlist.supabaseId) { podcasts ->
            val names = podcasts.joinToString("\n") { "• ${it.title} - ${it.author}" }
            val content = if (names.isEmpty()) "Esta lista está vacía" else names
            val queueIds = podcasts.map { it.id }.toIntArray()

            AlertDialog.Builder(requireContext())
                .setTitle(playlist.name)
                .setMessage(content)
                .setNeutralButton("Reproducir lista") { _, _ ->
                    if (queueIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Esta lista está vacía", Toast.LENGTH_SHORT).show()
                        return@setNeutralButton
                    }
                    openPlaylistQueue(queueIds)
                }
                .setPositiveButton("Añadir podcast") { _, _ ->
                    if (!SessionManager.isLoggedIn()) {
                        Toast.makeText(requireContext(), "Ingresa para modificar tus listas", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    showAddToPlaylistDialog(playlist)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
    }

    private fun openPlaylistQueue(queueIds: IntArray) {
        val root = view ?: return
        if (queueIds.isEmpty()) return
        val bundle = Bundle().apply {
            putInt("podcastId", queueIds[0])
            putIntArray(ARG_QUEUE_PODCAST_IDS, queueIds)
            putInt(ARG_QUEUE_INDEX, 0)
        }
        Navigation.findNavController(root).navigate(R.id.detailFragment, bundle)
    }

    private fun showAddToPlaylistDialog(playlist: Playlist) {
        val allPodcasts = viewModel.uiState.value.allPodcasts
        if (allPodcasts.isEmpty()) {
            Toast.makeText(requireContext(), "No hay podcasts disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val itemsResult = SupabaseService.getPlaylistItems(playlist.supabaseId)
            val currentPodcastIds = (itemsResult.getOrNull() ?: emptyList()).map { it.podcast_id }.toMutableSet()

            val podcastNames = allPodcasts.map { "${it.title} - ${it.author}" }.toTypedArray()
            val checked = allPodcasts.map { it.supabaseId in currentPodcastIds }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Añadir a \"${playlist.name}\"")
                .setMultiChoiceItems(podcastNames, checked) { _, which, isChecked ->
                    val podcast = allPodcasts[which]
                    if (isChecked) {
                        viewModel.addToPlaylist(playlist.supabaseId, podcast.supabaseId) {}
                    } else {
                        viewModel.removeFromPlaylist(playlist.supabaseId, podcast.supabaseId) {}
                    }
                }
                .setPositiveButton("Listo") { _, _ ->
                    viewModel.loadPlaylists()
                    Toast.makeText(requireContext(), "¡Lista actualizada!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun confirmDeletePlaylist(playlist: Playlist) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar lista")
            .setMessage("¿Eliminar \"${playlist.name}\"? Se eliminarán todos los podcasts guardados en ella.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deletePlaylist(playlist.supabaseId) { success ->
                    if (!success) {
                        Toast.makeText(requireContext(), "No pudimos eliminar la lista", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterDialog() {
        val categories = viewModel.uiState.value.allPodcasts.map { it.category }.distinct().toTypedArray()
        if (categories.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por categoría")
            .setItems(categories) { _, which ->
                val cat = categories[which]
                val filtered = viewModel.uiState.value.allPodcasts.filter { it.category == cat }
                binding.rvAllPodcasts.adapter = PodcastAdapter(
                    filtered,
                    onItemClick = ::openDetail,
                    onAuthorClick = ::openAuthorProfile
                )
                Toast.makeText(requireContext(), "Mostrando: $cat", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Mostrar todos") { _, _ ->
                binding.rvAllPodcasts.adapter = PodcastAdapter(
                    viewModel.uiState.value.allPodcasts,
                    onItemClick = ::openDetail,
                    onAuthorClick = ::openAuthorProfile
                )
            }
            .show()
    }

    private fun openDetail(podcast: Podcast) {
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(requireView()).navigate(R.id.detailFragment, bundle)
    }

    private fun openAuthorProfile(podcast: Podcast) {
        if (podcast.userId.isBlank()) return
        val bundle = Bundle().apply {
            putString("userId", podcast.userId)
            putString("authorName", podcast.author)
        }
        Navigation.findNavController(requireView()).navigate(R.id.userProfileFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
