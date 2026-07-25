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
    private var allPodcasts: List<Podcast> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategory: String? = null

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
            loadPodcasts(fromSwipeRefresh = true)
        }
        binding.edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }
        })
        binding.btnFiltro.setOnClickListener {
            showCategoryFilterDialog()
        }

        loadPodcasts(fromSwipeRefresh = false)
    }

    private fun loadPodcasts(fromSwipeRefresh: Boolean) {
        if (_binding == null) return
        if (fromSwipeRefresh) {
            binding.progressBar.visibility = View.GONE
            binding.swipeLayout.isRefreshing = true
        } else {
            binding.swipeLayout.isRefreshing = false
            setInitialLoading(true)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = SupabaseService.getAllPodcasts()
            if (_binding == null) return@launch
            binding.swipeLayout.isRefreshing = false
            if (!fromSwipeRefresh) {
                setInitialLoading(false)
            }
            if (result.isSuccess) {
                allPodcasts = result.getOrNull() ?: emptyList()
                applyFilters()
            } else {
                Toast.makeText(requireContext(), "No pudimos cargar los podcasts", Toast.LENGTH_SHORT).show()
                allPodcasts = emptyList()
                applyFilters()
            }
        }
    }

    private fun setInitialLoading(loading: Boolean) {
        if (_binding == null) return
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        val contentVisibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.edtBuscar.visibility = contentVisibility
        binding.cardBanner.visibility = contentVisibility
        binding.layoutDestacadosHeader.visibility = contentVisibility
        binding.swipeLayout.visibility = contentVisibility
    }

    private fun applyFilters() {
        if (_binding == null) return
        val filtered = allPodcasts.filter { podcast ->
            val matchesQuery = searchQuery.isBlank() ||
                podcast.title.contains(searchQuery, ignoreCase = true) ||
                podcast.author.contains(searchQuery, ignoreCase = true) ||
                podcast.description.contains(searchQuery, ignoreCase = true) ||
                podcast.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory.isNullOrBlank() || podcast.category == selectedCategory
            matchesQuery && matchesCategory
        }
        binding.rvPodcasts.adapter = PodcastAdapter(
            filtered,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile
        )
    }

    private fun showCategoryFilterDialog() {
        val categories = allPodcasts.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Aún no hay categorías disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Todas las categorías", *categories.toTypedArray())
        val selectedIndex = selectedCategory?.let { category ->
            categories.indexOf(category).takeIf { it >= 0 }?.plus(1)
        } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_title)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                selectedCategory = if (which == 0) null else categories[which - 1]
                applyFilters()
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
