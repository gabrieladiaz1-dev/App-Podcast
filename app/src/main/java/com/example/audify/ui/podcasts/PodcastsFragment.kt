package com.example.audify.ui.podcasts

import android.content.Intent
import android.os.Bundle
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.databinding.FragmentPodcastsBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.CategoryAdapter
import com.example.audify.ui.adapter.PodcastAdapter
import com.example.audify.viewmodel.PodcastsViewModel
import kotlinx.coroutines.launch

class PodcastsFragment : Fragment() {

    private var _binding: FragmentPodcastsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PodcastsViewModel by viewModels()

    private class NonScrollableLinearLayoutManager(context: android.content.Context) : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean = false
        override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
            try { super.onLayoutChildren(recycler, state) } catch (_: IndexOutOfBoundsException) {}
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadProfile()
        viewModel.loadUserPodcasts()
    }

    override fun onResume() {
        super.onResume()
        val state = viewModel.uiState.value
        if (state.selectedStatus != com.example.audify.viewmodel.StatusFilter.ALL || !state.selectedCategory.isNullOrBlank()) {
            viewModel.resetFilters()
        }
    }

    private fun bindState(state: com.example.audify.viewmodel.PodcastsUiState) {
        if (state.isLoading && state.allPodcasts.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.scrollContent.visibility = View.VISIBLE
        }

        binding.txtAvatar.text = state.profileName.firstOrNull()?.uppercase() ?: "?"
        binding.txtNombre.text = state.profileName
        binding.txtPodcastCount.text = state.allPodcasts.size.toString()
        binding.txtCategoryCount.text = "${state.approvedCount} aprobados · ${state.pendingCount} pendiente${if (state.pendingCount != 1) "s" else ""}"
        binding.txtSectionTitle.text = "Mis podcasts (${state.visiblePodcasts.size})"

        if (binding.rvCategories.layoutManager == null) {
            binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        }
        binding.rvCategories.adapter = CategoryAdapter(
            state.categoryCards,
            selectedKey = state.selectedKey,
            onCategoryClick = { item ->
                when (item.key) {
                    "status_all" -> viewModel.setStatusFilter(com.example.audify.viewmodel.StatusFilter.ALL)
                    "status_approved" -> viewModel.setStatusFilter(com.example.audify.viewmodel.StatusFilter.APPROVED)
                    "status_pending" -> viewModel.setStatusFilter(com.example.audify.viewmodel.StatusFilter.PENDING)
                    else -> viewModel.toggleCategory(item.title)
                }
            }
        )

        if (binding.rvUserPodcasts.layoutManager == null) {
            binding.rvUserPodcasts.layoutManager = NonScrollableLinearLayoutManager(requireContext())
        }
        binding.rvUserPodcasts.isNestedScrollingEnabled = false
        binding.rvUserPodcasts.adapter = PodcastAdapter(
            state.visiblePodcasts,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile,
            contentTopPaddingDp = 0,
            cardTopMarginDp = 2
        )
        binding.rvUserPodcasts.post { binding.rvUserPodcasts.requestLayout() }

        binding.txtEmptyCategories.visibility = if (state.allPodcasts.isEmpty()) View.VISIBLE else View.GONE
        binding.txtEmptyPodcasts.visibility = if (state.visiblePodcasts.isEmpty()) View.VISIBLE else View.GONE
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
