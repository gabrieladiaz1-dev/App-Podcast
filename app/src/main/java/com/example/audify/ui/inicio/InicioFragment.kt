package com.example.audify.ui.inicio

import android.app.AlertDialog
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.databinding.FragmentInicioBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.PodcastAdapter
import com.example.audify.viewmodel.InicioViewModel
import kotlinx.coroutines.launch

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InicioViewModel by viewModels()

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

        binding.rvPodcasts.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeLayout.setOnRefreshListener {
            viewModel.loadPodcasts(fromSwipeRefresh = true)
        }
        binding.edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString()?.trim().orEmpty())
            }
        })
        binding.btnFiltro.setOnClickListener {
            showCategoryFilterDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadPodcasts()
    }

    private fun bindState(state: com.example.audify.viewmodel.InicioUiState) {
        binding.swipeLayout.isRefreshing = state.isRefreshing
        if (state.isLoading && !state.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
            binding.edtBuscar.visibility = View.INVISIBLE
            binding.cardBanner.visibility = View.INVISIBLE
            binding.layoutDestacadosHeader.visibility = View.INVISIBLE
            binding.swipeLayout.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.edtBuscar.visibility = View.VISIBLE
            binding.cardBanner.visibility = View.VISIBLE
            binding.layoutDestacadosHeader.visibility = View.VISIBLE
            binding.swipeLayout.visibility = View.VISIBLE
        }

        binding.rvPodcasts.adapter = PodcastAdapter(
            state.filteredPodcasts,
            onItemClick = ::openDetail,
            onFavoriteClick = { podcast -> viewModel.toggleFavorite(podcast.supabaseId) },
            onAuthorClick = ::openAuthorProfile,
            favoriteIds = state.favoriteIds
        )
    }

    private fun showCategoryFilterDialog() {
        val categories = viewModel.getCategories()
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Aún no hay categorías disponibles", Toast.LENGTH_SHORT).show()
            return
        }
        val currentCategory = viewModel.uiState.value.selectedCategory
        val options = arrayOf("Todas las categorías", *categories.toTypedArray())
        val selectedIndex = currentCategory?.let { cat ->
            categories.indexOf(cat).takeIf { it >= 0 }?.plus(1)
        } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_title)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                viewModel.setSelectedCategory(if (which == 0) null else categories[which - 1])
                dialog.dismiss()
            }
            .setNegativeButton("Cerrar", null)
            .show()
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
