package com.example.audify.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.audify.R
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentUserProfileBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.CategoryAdapter
import com.example.audify.ui.adapter.CategoryUiItem
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private var allPodcasts: List<Podcast> = emptyList()
    private var selectedCategory: String? = null

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
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        val userId = arguments?.getString("userId") ?: return
        val fallbackName = arguments?.getString("authorName")
        loadProfile(userId, fallbackName)
    }

    private fun loadProfile(userId: String, fallbackName: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val profile = SupabaseService.getProfileByUserId(userId)
            binding.progressBar.visibility = View.GONE

            val displayName = profile?.name?.ifEmpty { null }
                ?: profile?.username?.ifBlank { null }
                ?: fallbackName?.ifBlank { null }
                ?: "Usuario"
            val avatarUrl = profile?.avatar_url
            if (!avatarUrl.isNullOrBlank()) {
                binding.imgAvatar.visibility = View.VISIBLE
                binding.txtAvatar.visibility = View.GONE
                binding.imgAvatar.load(avatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_circle_violet)
                    error(R.drawable.bg_circle_violet)
                }
            } else {
                binding.imgAvatar.visibility = View.GONE
                binding.txtAvatar.visibility = View.VISIBLE
                binding.txtAvatar.text = displayName.firstOrNull()?.uppercase() ?: "?"
            }
            binding.txtNombre.text = displayName

            val podcastsResult = SupabaseService.getPodcastsByUser(userId)
            allPodcasts = if (podcastsResult.isSuccess) podcastsResult.getOrNull() ?: emptyList() else emptyList()
            selectedCategory = null
            binding.txtPodcastCount.text = allPodcasts.size.toString()
            renderCategoryFiltersAndList()

            if (profile == null && allPodcasts.isEmpty()) {
                Toast.makeText(requireContext(), "No encontramos a ese usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderCategoryFiltersAndList() {
        if (selectedCategory != null) {
            val exists = allPodcasts.any { it.category.trim() == selectedCategory }
            if (!exists) selectedCategory = null
        }

        val categoryNames = allPodcasts.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val categoryCards = mutableListOf(
            CategoryUiItem("all", "Todos", allPodcasts.size)
        )
        categoryCards.addAll(
            categoryNames.map { categoryName ->
                val count = allPodcasts.count { it.category.trim() == categoryName }
                CategoryUiItem("cat_$categoryName", categoryName, count)
            }
        )

        val selectedKey = selectedCategory?.let { "cat_$it" } ?: "all"
        if (binding.rvCategories.layoutManager == null) {
            binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        }
        binding.rvCategories.adapter = CategoryAdapter(
            categoryCards,
            selectedKey = selectedKey,
            onCategoryClick = { item ->
                selectedCategory = when (item.key) {
                    "all" -> null
                    else -> if (selectedCategory == item.title) null else item.title
                }
                renderCategoryFiltersAndList()
            }
        )

        val visiblePodcasts = allPodcasts.filter { podcast ->
            selectedCategory.isNullOrBlank() || podcast.category.trim() == selectedCategory
        }

        binding.txtSectionTitle.text = "Podcasts (${visiblePodcasts.size})"
        if (binding.rvUserPodcasts.layoutManager == null) {
            binding.rvUserPodcasts.layoutManager = NonScrollableLinearLayoutManager(requireContext())
        }
        binding.rvUserPodcasts.isNestedScrollingEnabled = false
        binding.rvUserPodcasts.adapter = PodcastAdapter(
            visiblePodcasts,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile
        )
        binding.rvUserPodcasts.post { binding.rvUserPodcasts.requestLayout() }

        binding.txtEmptyCategories.visibility = if (categoryNames.isEmpty()) View.VISIBLE else View.GONE
        binding.txtEmpty.visibility = if (visiblePodcasts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openDetail(podcast: com.example.audify.model.Podcast) {
        val bundle = Bundle().apply { putInt("podcastId", podcast.id) }
        Navigation.findNavController(requireView()).navigate(R.id.detailFragment, bundle)
    }

    private fun openAuthorProfile(podcast: com.example.audify.model.Podcast) {
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
