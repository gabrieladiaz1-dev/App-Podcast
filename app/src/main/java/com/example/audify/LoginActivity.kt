package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.audify.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.init(this)

        // Verificar sesión local primero, luego Supabase
        if (SessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-cargar el cliente Supabase en background
        SupabaseService.preload()

        binding.etEmail.requestFocus()

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

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            var hasError = false

            if (email.isEmpty()) {
                binding.etEmail.requestFocus()
                Toast.makeText(this, "Ingresa tu correo electr\u00f3nico", Toast.LENGTH_SHORT).show()
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electr\u00f3nico inv\u00e1lido", Toast.LENGTH_SHORT).show()
                binding.etEmail.requestFocus()
                hasError = true
            }

            if (!hasError && password.isEmpty()) {
                Toast.makeText(this, "Ingresa tu contrase\u00f1a", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
                hasError = true
            }

            if (!hasError) {
                setLoadingState(true)
                lifecycleScope.launch {
                    SupabaseService.loginUser(email, password)
                        .onSuccess { userId ->
                            SessionManager.saveSession(email, userId)
                            navigateToMain()
                        }
                        .onFailure { error ->
                            setLoadingState(false)
                            val msg = error.message ?: "Error desconocido"
                            Toast.makeText(this@LoginActivity, "Error: $msg", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Te enviaremos un enlace para restablecer tu contrase\u00f1a", Toast.LENGTH_LONG).show()
        }

        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignIn.text = if (loading) "Ingresando..." else getString(R.string.btn_sign_in)
    }
}
