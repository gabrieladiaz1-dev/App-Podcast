package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.audify.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false
    private var isConfirmVisible = false

    private var pendingName = ""
    private var pendingUsername = ""
    private var pendingEmail = ""
    private var pendingPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SupabaseService.preload()

        binding.etFullName.requestFocus()

        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val selection = binding.etPassword.selectionStart
            binding.etPassword.transformationMethod =
                if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(selection)
            binding.btnTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility_off
                else R.drawable.ic_visibility
            )
        }

        binding.btnToggleConfirmPassword.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            val selection = binding.etConfirmPassword.selectionStart
            binding.etConfirmPassword.transformationMethod =
                if (isConfirmVisible) null else PasswordTransformationMethod.getInstance()
            binding.etConfirmPassword.setSelection(selection)
            binding.btnToggleConfirmPassword.setImageResource(
                if (isConfirmVisible) R.drawable.ic_visibility_off
                else R.drawable.ic_visibility
            )
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmailOrPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresa tu nombre completo", Toast.LENGTH_SHORT).show()
                binding.etFullName.requestFocus()
                hasError = true
            } else if (username.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
                binding.etUsername.requestFocus()
                hasError = true
            } else if (username.length < 3) {
                Toast.makeText(this, "El nombre de usuario debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show()
                binding.etUsername.requestFocus()
                hasError = true
            } else if (!username.matches(Regex("^[a-zA-Z0-9._]+$"))) {
                Toast.makeText(this, "El nombre de usuario solo puede contener letras, n\u00fameros, puntos y guiones bajos", Toast.LENGTH_SHORT).show()
                binding.etUsername.requestFocus()
                hasError = true
            } else if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo electr\u00f3nico", Toast.LENGTH_SHORT).show()
                binding.etEmailOrPhone.requestFocus()
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ingresa un correo electr\u00f3nico v\u00e1lido", Toast.LENGTH_SHORT).show()
                binding.etEmailOrPhone.requestFocus()
                hasError = true
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Crea una contrase\u00f1a", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
                hasError = true
            } else if (password.length < 6) {
                Toast.makeText(this, "La contrase\u00f1a debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
                hasError = true
            } else if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Confirma tu contrase\u00f1a", Toast.LENGTH_SHORT).show()
                binding.etConfirmPassword.requestFocus()
                hasError = true
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Las contrase\u00f1as no coinciden", Toast.LENGTH_SHORT).show()
                binding.etConfirmPassword.requestFocus()
                hasError = true
            }

            if (!hasError) {
                pendingName = name
                pendingUsername = username
                pendingEmail = email
                pendingPassword = password
                showConfirmationDialog()
            }
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.register_dialog_title)
            .setMessage(R.string.register_dialog_message)
            .setPositiveButton(R.string.register_dialog_accept) { _, _ ->
                registerWithSupabase()
            }
            .setNegativeButton(R.string.register_dialog_cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun registerWithSupabase() {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registrando..."
        lifecycleScope.launch {
            val available = SupabaseService.isUsernameAvailable(pendingUsername)
                .getOrNull() ?: true
            if (!available) {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = getString(R.string.btn_register)
                Toast.makeText(this@RegisterActivity, "Ese nombre de usuario ya est\u00e1 en uso", Toast.LENGTH_SHORT).show()
                binding.etUsername.requestFocus()
                return@launch
            }

            SupabaseService.registerUser(pendingEmail, pendingPassword)
                .onSuccess { userId ->
                    SupabaseService.createProfile(userId, pendingName, pendingUsername)
                    Toast.makeText(this@RegisterActivity, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
                .onFailure { error ->
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = getString(R.string.btn_register)
                    Toast.makeText(this@RegisterActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
