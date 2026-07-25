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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentPodcastsBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.CategoryAdapter
import com.example.audify.ui.adapter.CategoryUiItem
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class PodcastsFragment : Fragment() {

    private var _binding: FragmentPodcastsBinding? = null
    private val binding get() = _binding!!
    private var allPodcasts: List<Podcast> = emptyList()
    private var selectedCategory: String? = null
    private var selectedStatus: StatusFilter = StatusFilter.ALL

    private enum class StatusFilter {
        ALL, APPROVED, PENDING
    }

    private class NonScrollableLinearLayoutManager(context: android.content.Context) : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean = false

        override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (_: IndexOutOfBoundsException) {
                // Defensive guard for transient adapter updates during nested measurement.
            }
        }
    }

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
            requireActivity().finish()
            return
        }

        binding.btnMenu.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }

        loadProfile()
        loadUserPodcasts()
    }

    override fun onResume() {
        super.onResume()
        // Always return to unfiltered state when entering this screen.
        if (selectedStatus != StatusFilter.ALL || !selectedCategory.isNullOrBlank()) {
            selectedStatus = StatusFilter.ALL
            selectedCategory = null
            renderCategoryFiltersAndList()
        }
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
                allPodcasts = result.getOrNull() ?: emptyList()
                selectedStatus = StatusFilter.ALL
                selectedCategory = null
                val approved = allPodcasts.count { it.approved }
                val pending = allPodcasts.size - approved
                binding.txtPodcastCount.text = allPodcasts.size.toString()
                binding.txtCategoryCount.text = "$approved aprobados · $pending pendiente${if (pending != 1) "s" else ""}"
                renderCategoryFiltersAndList()
            } else {
                allPodcasts = emptyList()
                selectedStatus = StatusFilter.ALL
                selectedCategory = null
                binding.txtPodcastCount.text = "0"
                binding.txtCategoryCount.text = "0"
                binding.txtSectionTitle.text = "Mis podcasts (0)"
                renderCategoryFiltersAndList()
            }
        }
    }

    private fun renderCategoryFiltersAndList() {
        val approvedCount = allPodcasts.count { it.approved }
        val pendingCount = allPodcasts.size - approvedCount

        if (selectedCategory != null) {
            val categoryStillExists = allPodcasts.any { podcast ->
                podcast.category.trim().equals(selectedCategory, ignoreCase = false)
            }
            if (!categoryStillExists) selectedCategory = null
        }

        val statusFiltered = when (selectedStatus) {
            StatusFilter.ALL -> allPodcasts
            StatusFilter.APPROVED -> allPodcasts.filter { it.approved }
            StatusFilter.PENDING -> allPodcasts.filter { !it.approved }
        }

        val categoryNames = statusFiltered.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val categoryCards = mutableListOf(
            CategoryUiItem("status_all", "Todos", allPodcasts.size),
            CategoryUiItem("status_approved", "Aprobados", approvedCount),
            CategoryUiItem("status_pending", "En revision", pendingCount)
        )

        categoryCards.addAll(
            categoryNames.map { categoryName ->
                val count = statusFiltered.count { it.category.trim() == categoryName }
                CategoryUiItem("cat_$categoryName", categoryName, count)
            }
        )

        val selectedKey = when (selectedStatus) {
            StatusFilter.ALL -> "status_all"
            StatusFilter.APPROVED -> "status_approved"
            StatusFilter.PENDING -> "status_pending"
        }.takeIf { selectedCategory == null } ?: "cat_${selectedCategory.orEmpty()}"

        if (binding.rvCategories.layoutManager == null) {
            binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        }
        binding.rvCategories.adapter = CategoryAdapter(
            categoryCards,
            selectedKey = selectedKey,
            onCategoryClick = { item ->
                when (item.key) {
                    "status_all" -> {
                        selectedStatus = StatusFilter.ALL
                        selectedCategory = null
                    }
                    "status_approved" -> {
                        selectedStatus = StatusFilter.APPROVED
                        selectedCategory = null
                    }
                    "status_pending" -> {
                        selectedStatus = StatusFilter.PENDING
                        selectedCategory = null
                    }
                    else -> {
                        val category = item.title
                        selectedCategory = if (selectedCategory == category) null else category
                    }
                }
                renderCategoryFiltersAndList()
            }
        )

        val visiblePodcasts = statusFiltered.filter { podcast ->
            selectedCategory.isNullOrBlank() || podcast.category.trim() == selectedCategory
        }

        binding.txtSectionTitle.text = "Mis podcasts (${visiblePodcasts.size})"
        if (binding.rvUserPodcasts.layoutManager == null) {
            binding.rvUserPodcasts.layoutManager = NonScrollableLinearLayoutManager(requireContext())
        }
        binding.rvUserPodcasts.isNestedScrollingEnabled = false
        binding.rvUserPodcasts.adapter = PodcastAdapter(
            visiblePodcasts,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile,
            contentTopPaddingDp = 0,
            cardTopMarginDp = 2
        )
        binding.rvUserPodcasts.post { binding.rvUserPodcasts.requestLayout() }

        binding.txtEmptyCategories.visibility = if (allPodcasts.isEmpty()) View.VISIBLE else View.GONE
        binding.txtEmptyPodcasts.visibility = if (visiblePodcasts.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
