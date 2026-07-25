package com.example.audify.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.audify.R
import com.example.audify.databinding.FragmentUserProfileBinding
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.CategoryAdapter
import com.example.audify.ui.adapter.PodcastAdapter
import com.example.audify.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserProfileViewModel by viewModels()

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    if (_binding == null) return@collect
                    bindState(state)
                }
        }

        viewModel.loadProfile(userId, fallbackName)
    }

    private fun bindState(state: com.example.audify.viewmodel.UserProfileUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
            return
        }
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        if (!state.avatarUrl.isNullOrBlank()) {
            binding.imgAvatar.visibility = View.VISIBLE
            binding.txtAvatar.visibility = View.GONE
            binding.imgAvatar.load(state.avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_violet)
                error(R.drawable.bg_circle_violet)
            }
        } else {
            binding.imgAvatar.visibility = View.GONE
            binding.txtAvatar.visibility = View.VISIBLE
            binding.txtAvatar.text = state.displayName.firstOrNull()?.uppercase() ?: "?"
        }
        binding.txtNombre.text = state.displayName
        binding.txtPodcastCount.text = state.allPodcasts.size.toString()
        binding.txtSectionTitle.text = "Podcasts (${state.visiblePodcasts.size})"

        if (binding.rvCategories.layoutManager == null) {
            binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        }
        binding.rvCategories.adapter = CategoryAdapter(
            state.categoryCards,
            selectedKey = state.selectedKey,
            onCategoryClick = { item ->
                val category = item.title
                viewModel.toggleCategory(
                    if (item.key == "all") "" else category
                )
            }
        )

        if (binding.rvUserPodcasts.layoutManager == null) {
            binding.rvUserPodcasts.layoutManager = NonScrollableLinearLayoutManager(requireContext())
        }
        binding.rvUserPodcasts.isNestedScrollingEnabled = false
        binding.rvUserPodcasts.adapter = PodcastAdapter(
            state.visiblePodcasts,
            onItemClick = ::openDetail,
            onAuthorClick = ::openAuthorProfile
        )
        binding.rvUserPodcasts.post { binding.rvUserPodcasts.requestLayout() }

        binding.txtEmptyCategories.visibility = if (state.allPodcasts.isEmpty()) View.VISIBLE else View.GONE
        binding.txtEmpty.visibility = if (state.visiblePodcasts.isEmpty()) View.VISIBLE else View.GONE

        if (state.allPodcasts.isEmpty()) {
            Toast.makeText(requireContext(), "No encontramos a ese usuario", Toast.LENGTH_SHORT).show()
        }
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
