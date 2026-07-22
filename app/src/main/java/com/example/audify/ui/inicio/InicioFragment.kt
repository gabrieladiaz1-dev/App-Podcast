package com.example.audify.ui.inicio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.MockData
import com.example.audify.databinding.FragmentInicioBinding
import com.example.audify.ui.adapter.PodcastAdapter

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

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
        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), R.string.notif_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.edtBuscar.setOnEditorActionListener { _, _, _ ->
            val query = binding.edtBuscar.text.toString().trim()
            if (query.isNotEmpty()) {
                Toast.makeText(requireContext(), "Buscando: $query", Toast.LENGTH_SHORT).show()
            }
            false
        }
        binding.rvPodcasts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPodcasts.adapter = PodcastAdapter(MockData.getPodcasts())
        binding.swipeLayout.setOnRefreshListener {
            binding.rvPodcasts.adapter = PodcastAdapter(MockData.getPodcasts())
            binding.swipeLayout.isRefreshing = false
        }
        binding.btnFiltro.setOnClickListener {
            Toast.makeText(requireContext(), R.string.filter_title, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
