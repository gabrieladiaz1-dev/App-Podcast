package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.audify.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false
    private var isConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val email = binding.etEmailOrPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresa tu nombre completo", Toast.LENGTH_SHORT).show()
                binding.etFullName.requestFocus()
                hasError = true
            } else if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo o tel\u00e9fono", Toast.LENGTH_SHORT).show()
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
                binding.btnRegister.isEnabled = false
                lifecycleScope.launch {
                    SupabaseService.registerUser(email, password)
                        .onSuccess { userId ->
                            SupabaseService.createProfile(userId, name)
                            Toast.makeText(this@RegisterActivity, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                        .onFailure { error ->
                            Toast.makeText(this@RegisterActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    binding.btnRegister.isEnabled = true
                }
            }
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
