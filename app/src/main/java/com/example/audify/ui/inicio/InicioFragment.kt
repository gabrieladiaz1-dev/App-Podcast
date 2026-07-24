package com.example.audify.ui.inicio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentInicioBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), R.string.notif_coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.rvPodcasts.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeLayout.setOnRefreshListener {
            loadPodcasts()
        }
        binding.btnFiltro.setOnClickListener {
            Toast.makeText(requireContext(), R.string.filter_title, Toast.LENGTH_SHORT).show()
        }

        loadPodcasts()
    }

    private fun loadPodcasts() {
        binding.swipeLayout.isRefreshing = true
        lifecycleScope.launch {
            val result = SupabaseService.getAllPodcasts()
            binding.swipeLayout.isRefreshing = false
            if (result.isSuccess) {
                val podcasts = result.getOrNull() ?: emptyList()
                binding.rvPodcasts.adapter = PodcastAdapter(podcasts, ::openDetail)
            } else {
                Toast.makeText(requireContext(), "No pudimos cargar los podcasts", Toast.LENGTH_SHORT).show()
                binding.rvPodcasts.adapter = PodcastAdapter(emptyList(), ::openDetail)
            }
        }
    }

    private fun openDetail(podcast: Podcast) {
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(requireView()).navigate(R.id.detailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
