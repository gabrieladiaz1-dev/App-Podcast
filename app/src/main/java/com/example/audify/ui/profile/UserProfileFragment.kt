package com.example.audify.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.audify.R
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentUserProfileBinding
import com.example.audify.ui.adapter.PodcastAdapter
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

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
            val podcasts = if (podcastsResult.isSuccess) podcastsResult.getOrNull() ?: emptyList() else emptyList()
            val categories = podcasts.map { it.category.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            binding.txtPodcastCount.text = podcasts.size.toString()
            binding.txtSectionTitle.text = "Podcasts (${podcasts.size})"
            binding.rvUserPodcasts.layoutManager = LinearLayoutManager(requireContext())
            binding.rvUserPodcasts.adapter = PodcastAdapter(
                podcasts,
                onItemClick = ::openDetail,
                onAuthorClick = ::openAuthorProfile
            )
            binding.txtEmptyCategories.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE

            if (profile == null && podcasts.isEmpty()) {
                Toast.makeText(requireContext(), "No encontramos a ese usuario", Toast.LENGTH_SHORT).show()
            }

            if (podcasts.isEmpty()) {
                binding.txtEmpty.visibility = View.VISIBLE
            } else {
                binding.txtEmpty.visibility = View.GONE
            }
        }
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
