package com.example.audify.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.databinding.FragmentProfileBinding

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

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val name = "Usuario Audify"
        val email = "usuario@audify.com"

        binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
        binding.txtAvatar.setBackgroundResource(R.drawable.bg_circle_violet)
        binding.txtNombreDisplay.text = name
        binding.txtCorreo.text = email
        binding.edtNombre.setText(name)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { /* no-op, es un tab */ }

        binding.btnNotificacion.setOnClickListener {
            Toast.makeText(requireContext(), "Pr\u00f3ximamente", Toast.LENGTH_SHORT).show()
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

            binding.txtNombreDisplay.text = name
            binding.txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"

            binding.edtPassword.text.clear()
            binding.edtConfirmPassword.text.clear()

            Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
        }

        binding.btnCerrarSesion.setOnClickListener {
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
