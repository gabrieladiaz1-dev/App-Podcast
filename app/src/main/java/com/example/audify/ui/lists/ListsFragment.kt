package com.example.audify.ui.lists

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.MockData
import com.example.audify.databinding.FragmentListsBinding
import com.example.audify.model.Playlist
import com.example.audify.ui.adapter.PlaylistAdapter
import com.example.audify.ui.adapter.PodcastAdapter

class ListsFragment : Fragment() {

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!

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
        refreshPlaylists()
        setupAllPodcasts()
        setupCreateList()
    }

    private fun setupToolbar() {
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
    }

    private fun refreshPlaylists() {
        val items = MockData.playlists.toList()
        binding.txtTotalLists.text = items.size.toString()
        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylists.adapter = PlaylistAdapter(items) { playlist ->
            showPlaylistDetail(playlist)
        }
    }

    private fun setupAllPodcasts() {
        binding.rvAllPodcasts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllPodcasts.adapter = PodcastAdapter(MockData.getPodcasts())
    }

    private fun setupCreateList() {
        binding.btnCreateList.setOnClickListener { showCreateListDialog() }
    }

    private fun showCreateListDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nombre de la lista"
            setTextColor(0xFF1E1B4B.toInt())
            setHintTextColor(0xFFA78BFA.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Crear nueva lista")
            .setMessage("Escribe un nombre para tu lista de podcasts")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    MockData.createPlaylist(name)
                    refreshPlaylists()
                    Toast.makeText(requireContext(), "Lista \"$name\" creada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Escribe un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPlaylistDetail(playlist: Playlist) {
        val podcasts = playlist.podcastIds.mapNotNull { id ->
            MockData.getPodcasts().find { it.id == id }
        }
        val names = podcasts.joinToString("\n") { "\u2022 ${it.title} - ${it.author}" }
        val content = if (names.isEmpty()) "Esta lista est\u00e1 vac\u00eda" else names

        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setMessage(content)
            .setPositiveButton("A\u00f1adir podcast") { _, _ -> showAddToPlaylistDialog(playlist) }
            .setNeutralButton("Cerrar", null)
            .show()
    }

    private fun showAddToPlaylistDialog(playlist: Playlist) {
        val allPodcasts = MockData.getPodcasts()
        val names = allPodcasts.map { "${it.title} - ${it.author}" }.toTypedArray()
        val checked = allPodcasts.map { it.id in playlist.podcastIds }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("A\u00f1adir a \"${playlist.name}\"")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                val podcastId = allPodcasts[which].id
                if (isChecked) {
                    MockData.addPodcastToPlaylist(playlist.id, podcastId)
                } else {
                    MockData.removePodcastFromPlaylist(playlist.id, podcastId)
                }
            }
            .setPositiveButton("Listo") { _, _ ->
                refreshPlaylists()
                Toast.makeText(requireContext(), "Lista actualizada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterDialog() {
        val categories = MockData.getCategories().toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por categor\u00eda")
            .setItems(categories) { _, which ->
                val cat = categories[which]
                val filtered = MockData.getPodcasts().filter { it.category == cat }
                binding.rvAllPodcasts.adapter = PodcastAdapter(filtered)
                Toast.makeText(requireContext(), "Filtrando: $cat", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Mostrar todos") { _, _ ->
                binding.rvAllPodcasts.adapter = PodcastAdapter(MockData.getPodcasts())
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
