package com.example.audify.ui.podcasts

import android.content.Intent
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
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentPodcastsBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class PodcastsFragment : Fragment() {

    private var _binding: FragmentPodcastsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPodcastsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }

        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), R.string.notif_coming_soon, Toast.LENGTH_SHORT).show()
        }

        loadProfile()
        loadUserPodcasts()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val profile = SupabaseService.getProfile()
                val name = profile.name.ifEmpty { "Usuario" }
                binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
                binding.txtNombre.text = name
            } catch (e: Exception) {
                binding.txtAvatar.text = "?"
                binding.txtNombre.text = "Usuario"
            }
        }
    }

    private fun loadUserPodcasts() {
        lifecycleScope.launch {
            val result = SupabaseService.getUserPodcasts()
            if (result.isSuccess) {
                val podcasts = result.getOrNull() ?: emptyList()
                val approved = podcasts.count { it.approved }
                val pending = podcasts.size - approved
                binding.txtPodcastCount.text = podcasts.size.toString()
                binding.txtCategoryCount.text = "$approved aprobados"
                binding.txtSectionTitle.text = "Mis podcasts (${podcasts.size})"
                binding.rvUserPodcasts.layoutManager = LinearLayoutManager(requireContext())
                binding.rvUserPodcasts.adapter = PodcastAdapter(podcasts, ::openDetail)
            } else {
                binding.txtPodcastCount.text = "0"
                binding.txtCategoryCount.text = "0"
                binding.txtSectionTitle.text = "Mis podcasts (0)"
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
