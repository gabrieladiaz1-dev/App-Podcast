package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        if (SessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                Toast.makeText(this, "Escribe tu correo para poder ingresar", Toast.LENGTH_SHORT).show()
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ese correo no se ve bien. ¿Lo escribiste completo?", Toast.LENGTH_SHORT).show()
                binding.etEmail.requestFocus()
                hasError = true
            }

            if (!hasError && password.isEmpty()) {
                Toast.makeText(this, "¿Y tu contraseña? No la dejaste", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@LoginActivity, "No pudimos ingresar. ¿Correo o contraseña incorrectos?", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Próximamente podrás recuperar tu contraseña desde aquí", Toast.LENGTH_LONG).show()
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
        binding.btnSignIn.text = if (loading) "Entrando..." else getString(R.string.btn_sign_in)
    }
}
