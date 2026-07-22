package com.example.audify.ui.favorites

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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.MockData
import com.example.audify.databinding.FragmentFavoritesBinding
import com.example.audify.ui.adapter.PodcastAdapter

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

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

        setupToolbar()
        setupSearch()
        refreshFavorites()
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val all = MockData.getFavoritePodcasts()
                val filtered = if (query.isEmpty()) all
                    else all.filter {
                        it.title.lowercase().contains(query) ||
                        it.author.lowercase().contains(query) ||
                        it.category.lowercase().contains(query)
                    }
                binding.rvFavorites.adapter = PodcastAdapter(filtered, ::openDetail)
                binding.txtSectionTitle.text = "${getString(R.string.fav_section_title)} (${filtered.size})"
            }
        })
    }

    private fun setupToolbar() {
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }
        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), R.string.notif_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshFavorites() {
        val favorites = MockData.getFavoritePodcasts()
        binding.txtFavoriteCount.text = favorites.size.toString()
        binding.txtSectionTitle.text = "${getString(R.string.fav_section_title)} (${favorites.size})"
        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavorites.adapter = PodcastAdapter(favorites, ::openDetail) {
            binding.txtFavoriteCount.text = MockData.getFavoritePodcasts().size.toString()
        }
    }

    private fun openDetail(podcast: com.example.audify.model.Podcast) {
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(requireView()).navigate(R.id.detailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
