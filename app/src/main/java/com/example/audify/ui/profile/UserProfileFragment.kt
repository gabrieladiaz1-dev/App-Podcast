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
import com.example.audify.R
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentUserProfileBinding
import com.example.audify.ui.adapter.CategoryAdapter
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
        loadProfile(userId)
    }

    private fun loadProfile(userId: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val profile = SupabaseService.getProfileByUserId(userId)
            binding.progressBar.visibility = View.GONE

            if (profile == null) {
                Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val displayName = profile.name.ifEmpty { "Usuario" }
            binding.txtAvatar.text = displayName.firstOrNull()?.uppercase() ?: "?"
            binding.txtNombre.text = displayName
<<<<<<< HEAD
            binding.txtUsername.text = "@${profile.username.ifEmpty { "usuario" }}"
=======
>>>>>>> 1b10f94c7f0acd7d0da8896266b4e4f50e09e020
            binding.txtPodcastCount.text = "0"
            binding.txtCategoryCount.text = "0"
            binding.txtSectionTitle.text = "Podcasts (0)"
            binding.txtEmpty.visibility = View.VISIBLE
            binding.rvCategories.adapter = CategoryAdapter(emptyList())
            binding.rvUserPodcasts.adapter = PodcastAdapter(emptyList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
