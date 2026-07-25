package com.example.audify.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.databinding.FragmentProfileBinding
import com.example.audify.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

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

        setupClickListeners()
        viewModel.loadProfile()
    }

    private fun bindState(state: com.example.audify.viewmodel.ProfileUiState) {
        if (state.isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.INVISIBLE
            return
        }
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        binding.txtAvatar.text = state.name.firstOrNull()?.uppercase() ?: "?"
        binding.txtNombreDisplay.text = state.name
        binding.txtCorreo.text = state.email
        binding.edtNombre.setText(state.name)
    }

    private fun setupClickListeners() {
        binding.btnGuardar.setOnClickListener {
            val name = binding.edtNombre.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirmPassword = binding.edtConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Deja tu nombre para guardar", Toast.LENGTH_SHORT).show()
                binding.edtNombre.requestFocus()
                return@setOnClickListener
            }

            if (password.isNotEmpty() && password.length < 6) {
                Toast.makeText(requireContext(), "Tu contraseña nueva debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                binding.edtPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden. Revísalas", Toast.LENGTH_SHORT).show()
                binding.edtConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            viewModel.updateProfile(
                name,
                onSuccess = {
                    binding.txtNombreDisplay.text = name
                    binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
                    binding.edtPassword.text.clear()
                    binding.edtConfirmPassword.text.clear()
                    Toast.makeText(requireContext(), "¡Listo! Tus cambios se guardaron", Toast.LENGTH_SHORT).show()
                },
                onError = { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() }
            )
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
