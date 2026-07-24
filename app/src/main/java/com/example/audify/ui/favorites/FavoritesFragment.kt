package com.example.audify.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.audify.databinding.FragmentFavoritesBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private var allFavorites: List<Podcast> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }

        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), R.string.notif_coming_soon, Toast.LENGTH_SHORT).show()
        }
        setupSearch()
        loadFavorites()
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) allFavorites
                else allFavorites.filter {
                    it.title.lowercase().contains(query) ||
                    it.author.lowercase().contains(query) ||
                    it.description.lowercase().contains(query)
                }
                binding.rvFavorites.adapter = PodcastAdapter(filtered, ::openDetail)
                binding.txtSectionTitle.text = "Favoritos (${filtered.size})"
                binding.txtFavoriteCount.text = filtered.size.toString()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.isLoggedIn()) {
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        val userId = SessionManager.getUserId() ?: return
        lifecycleScope.launch {
            val result = SupabaseService.getFavoritePodcasts(userId)
            if (result.isSuccess) {
                allFavorites = result.getOrNull() ?: emptyList()
                binding.txtFavoriteCount.text = allFavorites.size.toString()
                binding.txtSectionTitle.text = "Favoritos (${allFavorites.size})"
                binding.rvFavorites.adapter = PodcastAdapter(allFavorites, ::openDetail)
            } else {
                Toast.makeText(requireContext(), "No pudimos cargar tus favoritos", Toast.LENGTH_SHORT).show()
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
