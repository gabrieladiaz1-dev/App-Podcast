package com.example.audify.ui.drafts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.DraftsManager
import com.example.audify.databinding.FragmentDraftsBinding
import com.example.audify.ui.adapter.DraftsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DraftsFragment : Fragment() {

    private var _binding: FragmentDraftsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDraftsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DraftsManager.init(requireContext())

        binding.btnBack.setOnClickListener {
            val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawer.openDrawer(GravityCompat.START)
        }

        loadDrafts()
    }

    override fun onResume() {
        super.onResume()
        loadDrafts()
    }

    private fun loadDrafts() {
        if (_binding == null) return
        setContentLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val drafts = withContext(Dispatchers.IO) { DraftsManager.getAllDrafts() }
                if (_binding == null) return@launch
                if (drafts.isEmpty()) {
                    binding.rvDrafts.visibility = View.GONE
                    binding.txtEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvDrafts.visibility = View.VISIBLE
                    binding.txtEmpty.visibility = View.GONE
                    binding.rvDrafts.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvDrafts.adapter = DraftsAdapter(drafts, ::openDraft, ::confirmDeleteDraft)
                }
            } finally {
                if (_binding != null) {
                    setContentLoading(false)
                }
            }
        }
    }

    private fun setContentLoading(loading: Boolean) {
        if (_binding == null) return
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.rvDrafts.visibility = View.INVISIBLE
            binding.txtEmpty.visibility = View.INVISIBLE
        }
    }

    private fun openDraft(draft: DraftsManager.Draft) {
        val bundle = Bundle().apply { putString("draftId", draft.id) }
        Navigation.findNavController(requireView()).navigate(R.id.uploadFragment, bundle)
    }

    private fun confirmDeleteDraft(draft: DraftsManager.Draft) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar borrador")
            .setMessage("¿Eliminar \"${draft.title.ifEmpty { "Sin título" }}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                DraftsManager.deleteDraft(requireContext(), draft.id)
                loadDrafts()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
