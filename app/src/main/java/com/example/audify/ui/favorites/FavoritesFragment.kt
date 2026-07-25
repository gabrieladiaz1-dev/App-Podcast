package com.example.audify.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.audify.databinding.FragmentFavoritesBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PodcastAdapter
import com.example.audify.viewmodel.FavoritesViewModel
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()

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
            requireActivity().finish()
            return
        }

        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString()?.trim() ?: "")
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.isLoggedIn()) {
            viewModel.loadFavorites()
        }
    }

    private fun bindState(state: com.example.audify.viewmodel.FavoritesUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
            return
        }
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        binding.txtFavoriteCount.text = state.filteredFavorites.size.toString()
        binding.txtSectionTitle.text = "Favoritos (${state.filteredFavorites.size})"
        binding.rvFavorites.adapter = PodcastAdapter(
            state.filteredFavorites,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile
        )
    }

    private fun openDetail(podcast: Podcast) {
        val root = view ?: return
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(root).navigate(R.id.detailFragment, bundle)
    }

    private fun openAuthorProfile(podcast: Podcast) {
        if (podcast.userId.isBlank()) return
        val root = view ?: return
        val bundle = Bundle().apply {
            putString("userId", podcast.userId)
            putString("authorName", podcast.author)
        }
        Navigation.findNavController(root).navigate(R.id.userProfileFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
