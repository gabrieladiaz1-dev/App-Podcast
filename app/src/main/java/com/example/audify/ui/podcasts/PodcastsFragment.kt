package com.example.audify.ui.podcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.MockData
import com.example.audify.databinding.FragmentPodcastsBinding
import com.example.audify.ui.adapter.CategoryAdapter
import com.example.audify.ui.adapter.PodcastAdapter

class PodcastsFragment : Fragment() {

    private var _binding: FragmentPodcastsBinding? = null
    private val binding get() = _binding!!

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

        setupToolbar()
        loadProfile()
        setupCategories()
        setupUserPodcasts()
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

    private fun loadProfile() {
        val name = "Gabriela D\u00edaz"
        binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
        binding.txtNombre.text = name

        val userPodcasts = MockData.getUserPodcasts()
        val categories = userPodcasts.map { it.category }.distinct()

        binding.txtPodcastCount.text = userPodcasts.size.toString()
        binding.txtCategoryCount.text = categories.size.toString()
    }

    private fun setupCategories() {
        val categories = MockData.getCategories()
        binding.rvCategories.layoutManager =
            GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = CategoryAdapter(categories)
    }

    private fun setupUserPodcasts() {
        val userPodcasts = MockData.getUserPodcasts()
        binding.txtSectionTitle.text = getString(R.string.bottom_podcasts) + " (${userPodcasts.size})"
        binding.rvUserPodcasts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUserPodcasts.adapter = PodcastAdapter(userPodcasts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
