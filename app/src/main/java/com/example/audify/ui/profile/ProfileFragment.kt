package com.example.audify.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
            return
        }

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val profile = SupabaseService.getProfile()
                val name = profile.name.ifEmpty { email.substringBefore("@").ifEmpty { "Usuario" } }
                binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
                binding.txtNombreDisplay.text = name
                binding.txtCorreo.text = email
                binding.edtNombre.setText(name)
            } catch (e: Exception) {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val fallback = email.substringBefore("@").ifEmpty { "Usuario" }
                binding.txtAvatar.text = fallback.firstOrNull()?.uppercase() ?: "?"
                binding.txtNombreDisplay.text = fallback
                binding.txtCorreo.text = email
                binding.edtNombre.setText(fallback)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { /* no-op, es un tab */ }

        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.notif_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnGuardar.setOnClickListener {
            val name = binding.edtNombre.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirmPassword = binding.edtConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre no puede estar vac\u00edo", Toast.LENGTH_SHORT).show()
                binding.edtNombre.requestFocus()
                return@setOnClickListener
            }

            if (password.isNotEmpty() && password.length < 6) {
                Toast.makeText(requireContext(), "La contrase\u00f1a debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                binding.edtPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Las contrase\u00f1as no coinciden", Toast.LENGTH_SHORT).show()
                binding.edtConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                var success = true
                var errorMsg = ""

                SupabaseService.updateProfileName(name).onFailure {
                    success = false
                    errorMsg = it.message ?: "Error al guardar nombre"
                }

                if (success) {
                    binding.txtNombreDisplay.text = name
                    binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
                    binding.edtPassword.text.clear()
                    binding.edtConfirmPassword.text.clear()
                    Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnCerrarSesion.setOnClickListener {
            SessionManager.clearSession()
            lifecycleScope.launch {
                SupabaseService.signOut()
            }
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
