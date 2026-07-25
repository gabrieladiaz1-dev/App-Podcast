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
import kotlinx.coroutines.launch

class ListsFragment : Fragment() {

    companion object {
        const val ARG_QUEUE_PODCAST_IDS = "queuePodcastIds"
        const val ARG_QUEUE_INDEX = "queueIndex"
    }

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!

    private var allApprovedPodcasts: List<Podcast> = emptyList()
    private var activeLoadCount = 0

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
        loadPlaylists()
        loadAllPodcasts()
    }

    override fun onResume() {
        super.onResume()
        loadPlaylists()
    }

    private fun setupToolbar() {
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
    }

    private fun loadPlaylists() {
        if (_binding == null) return
        if (!SessionManager.isLoggedIn()) {
            binding.txtTotalLists.text = "0"
            binding.rvPlaylists.adapter = PlaylistAdapter(emptyList(), {}, null)
            return
        }
        setContentLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!ensureListsSessionOrRedirect()) {
                    if (_binding != null) {
                        binding.txtTotalLists.text = "0"
                        binding.rvPlaylists.adapter = PlaylistAdapter(emptyList(), {}, null)
                    }
                    return@launch
                }
                val result = SupabaseService.getUserPlaylists()
                if (_binding == null) return@launch
                if (result.isSuccess) {
                    val playlists = result.getOrNull() ?: emptyList()
                    binding.txtTotalLists.text = playlists.size.toString()
                    val modelPlaylists = playlists.map { ps ->
                        val itemsResult = SupabaseService.getPlaylistItems(ps.id)
                        val count = itemsResult.getOrNull()?.size ?: 0
                        Playlist(
                            id = ps.id.hashCode(),
                            supabaseId = ps.id,
                            name = ps.name,
                            podcastCount = count
                        )
                    }
                    binding.rvPlaylists.adapter = PlaylistAdapter(modelPlaylists, ::showPlaylistDetail, ::confirmDeletePlaylist)
                } else {
                    binding.txtTotalLists.text = "0"
                    val msg = result.exceptionOrNull()?.message ?: "No pudimos cargar tus listas"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (_binding != null) {
                    setContentLoading(false)
                }
            }
        }
    }

    private fun loadAllPodcasts() {
        if (_binding == null) return
        setContentLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = SupabaseService.getAllPodcasts()
                if (_binding == null) return@launch
                if (result.isSuccess) {
                    allApprovedPodcasts = result.getOrNull() ?: emptyList()
                    binding.rvAllPodcasts.adapter = PodcastAdapter(
                        allApprovedPodcasts,
                        onItemClick = ::openDetail,
                        onAuthorClick = ::openAuthorProfile
                    )
                }
            } finally {
                if (_binding != null) {
                    setContentLoading(false)
                }
            }
        }
    }

    private fun setContentLoading(loading: Boolean) {
        if (_binding == null) return
        if (loading) {
            activeLoadCount += 1
        } else {
            activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
        }
        val showLoading = activeLoadCount > 0
        binding.progressBar.visibility = if (showLoading) View.VISIBLE else View.GONE
        binding.layoutHeader.visibility = if (showLoading) View.INVISIBLE else View.VISIBLE
        binding.scrollContent.visibility = if (showLoading) View.INVISIBLE else View.VISIBLE
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
                    createPlaylist(name)
                } else {
                    Toast.makeText(requireContext(), "¿Cómo se llama tu lista?", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createPlaylist(name: String) {
        lifecycleScope.launch {
            if (!ensureListsSessionOrRedirect()) return@launch
            val result = SupabaseService.createPlaylist(name)
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "¡Lista \"$name\" creada!", Toast.LENGTH_SHORT).show()
                loadPlaylists()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "No pudimos crear la lista. Intenta de nuevo"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPlaylistDetail(playlist: Playlist) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = SupabaseService.getPlaylistPodcasts(playlist.supabaseId)
            val podcasts = if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()

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
        lifecycleScope.launch {
            if (!ensureListsSessionOrRedirect()) return@launch
            val itemsResult = SupabaseService.getPlaylistItems(playlist.supabaseId)
            val currentPodcastIds = (itemsResult.getOrNull() ?: emptyList()).map { it.podcast_id }.toMutableSet()

            if (allApprovedPodcasts.isEmpty()) {
                Toast.makeText(requireContext(), "No hay podcasts disponibles", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val podcastNames = allApprovedPodcasts.map { "${it.title} - ${it.author}" }.toTypedArray()
            val checked = allApprovedPodcasts.map { it.supabaseId in currentPodcastIds }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Añadir a \"${playlist.name}\"")
                .setMultiChoiceItems(podcastNames, checked) { _, which, isChecked ->
                    val podcast = allApprovedPodcasts[which]
                    lifecycleScope.launch {
                        val result = if (isChecked) {
                            SupabaseService.addPodcastToPlaylist(playlist.supabaseId, podcast.supabaseId)
                        } else {
                            SupabaseService.removePodcastFromPlaylist(playlist.supabaseId, podcast.supabaseId)
                        }
                        if (result.isFailure) {
                            val msg = result.exceptionOrNull()?.message ?: "No pudimos actualizar la lista"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setPositiveButton("Listo") { _, _ ->
                    loadPlaylists()
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
                lifecycleScope.launch {
                    val result = SupabaseService.deletePlaylist(playlist.supabaseId)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Lista eliminada", Toast.LENGTH_SHORT).show()
                        loadPlaylists()
                    } else {
                        Toast.makeText(requireContext(), "No pudimos eliminar la lista", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun ensureListsSessionOrRedirect(): Boolean {
        val valid = SupabaseService.ensureValidSession()
        if (valid) return true

        SessionManager.clearSession()
        Toast.makeText(
            requireContext(),
            "Tu sesión expiró. Inicia sesión para usar tus listas",
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
        return false
    }

    private fun showFilterDialog() {
        val categories = allApprovedPodcasts.map { it.category }.distinct().toTypedArray()
        if (categories.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por categoría")
            .setItems(categories) { _, which ->
                val cat = categories[which]
                val filtered = allApprovedPodcasts.filter { it.category == cat }
                binding.rvAllPodcasts.adapter = PodcastAdapter(
                    filtered,
                    onItemClick = ::openDetail,
                    onAuthorClick = ::openAuthorProfile
                )
                Toast.makeText(requireContext(), "Mostrando: $cat", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Mostrar todos") { _, _ ->
                binding.rvAllPodcasts.adapter = PodcastAdapter(
                    allApprovedPodcasts,
                    onItemClick = ::openDetail,
                    onAuthorClick = ::openAuthorProfile
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeLoadCount = 0
        _binding = null
    }
}
