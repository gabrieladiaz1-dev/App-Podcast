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

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!

    private var allApprovedPodcasts: List<Podcast> = emptyList()

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
        if (!SessionManager.isLoggedIn()) {
            binding.txtTotalLists.text = "0"
            binding.rvPlaylists.adapter = PlaylistAdapter(emptyList(), {}, null)
            return
        }
        lifecycleScope.launch {
            val result = SupabaseService.getUserPlaylists()
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
                binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
                binding.rvPlaylists.adapter = PlaylistAdapter(modelPlaylists, ::showPlaylistDetail, ::confirmDeletePlaylist)
            } else {
                binding.txtTotalLists.text = "0"
                Toast.makeText(requireContext(), "No pudimos cargar tus listas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAllPodcasts() {
        lifecycleScope.launch {
            val result = SupabaseService.getAllPodcasts()
            if (result.isSuccess) {
                allApprovedPodcasts = result.getOrNull() ?: emptyList()
                binding.rvAllPodcasts.layoutManager = LinearLayoutManager(requireContext())
                binding.rvAllPodcasts.adapter = PodcastAdapter(allApprovedPodcasts, ::openDetail)
            }
        }
    }

    private fun openDetail(podcast: Podcast) {
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(requireView()).navigate(R.id.detailFragment, bundle)
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
            val result = SupabaseService.createPlaylist(name)
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "¡Lista \"$name\" creada!", Toast.LENGTH_SHORT).show()
                loadPlaylists()
            } else {
                Toast.makeText(requireContext(), "No pudimos crear la lista. Intenta de nuevo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPlaylistDetail(playlist: Playlist) {
        lifecycleScope.launch {
            val result = SupabaseService.getPlaylistPodcasts(playlist.supabaseId)
            val podcasts = if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()

            val names = podcasts.joinToString("\n") { "• ${it.title} - ${it.author}" }
            val content = if (names.isEmpty()) "Esta lista está vacía" else names

            AlertDialog.Builder(requireContext())
                .setTitle(playlist.name)
                .setMessage(content)
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

    private fun showAddToPlaylistDialog(playlist: Playlist) {
        lifecycleScope.launch {
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
                        if (isChecked) {
                            SupabaseService.addPodcastToPlaylist(playlist.supabaseId, podcast.supabaseId)
                        } else {
                            SupabaseService.removePodcastFromPlaylist(playlist.supabaseId, podcast.supabaseId)
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

    private fun showFilterDialog() {
        val categories = allApprovedPodcasts.map { it.category }.distinct().toTypedArray()
        if (categories.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por categoría")
            .setItems(categories) { _, which ->
                val cat = categories[which]
                val filtered = allApprovedPodcasts.filter { it.category == cat }
                binding.rvAllPodcasts.adapter = PodcastAdapter(filtered, ::openDetail)
                Toast.makeText(requireContext(), "Mostrando: $cat", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Mostrar todos") { _, _ ->
                binding.rvAllPodcasts.adapter = PodcastAdapter(allApprovedPodcasts, ::openDetail)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
