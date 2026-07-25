package com.example.audify.ui.drafts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audify.R
import com.example.audify.data.DraftsManager
import com.example.audify.databinding.FragmentDraftsBinding
import com.example.audify.ui.adapter.DraftsAdapter
import com.example.audify.viewmodel.DraftsViewModel
import kotlinx.coroutines.launch

class DraftsFragment : Fragment() {

    private var _binding: FragmentDraftsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DraftsViewModel by viewModels()

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

        binding.btnBack.setOnClickListener {
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

        viewModel.loadDrafts(requireContext())
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDrafts(requireContext())
    }

    private fun bindState(state: com.example.audify.viewmodel.DraftsUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvDrafts.visibility = View.INVISIBLE
            binding.txtEmpty.visibility = View.INVISIBLE
            return
        }
        binding.progressBar.visibility = View.GONE

        if (state.drafts.isEmpty()) {
            binding.rvDrafts.visibility = View.GONE
            binding.txtEmpty.visibility = View.VISIBLE
        } else {
            binding.rvDrafts.visibility = View.VISIBLE
            binding.txtEmpty.visibility = View.GONE
            binding.rvDrafts.layoutManager = LinearLayoutManager(requireContext())
            binding.rvDrafts.adapter = DraftsAdapter(
                state.drafts,
                ::openDraft,
                ::confirmDeleteDraft
            )
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
                viewModel.deleteDraft(requireContext(), draft.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
