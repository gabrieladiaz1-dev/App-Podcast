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
            val email = binding.etEmailOrPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                Toast.makeText(this, "¿Cómo te llamas? Deja tu nombre completo", Toast.LENGTH_SHORT).show()
                binding.etFullName.requestFocus()
                hasError = true
            } else if (email.isEmpty()) {
                Toast.makeText(this, "¿Cuál es tu correo? Lo necesitamos para tu cuenta", Toast.LENGTH_SHORT).show()
                binding.etEmailOrPhone.requestFocus()
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ese correo no parece válido. ¿Lo escribiste bien?", Toast.LENGTH_SHORT).show()
                binding.etEmailOrPhone.requestFocus()
                hasError = true
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Crea una contraseña para proteger tu cuenta", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
                hasError = true
            } else if (password.length < 6) {
                Toast.makeText(this, "Tu contraseña debe tener al menos 6 letras o números", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
                hasError = true
            } else if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Escribe tu contraseña otra vez para confirmar", Toast.LENGTH_SHORT).show()
                binding.etConfirmPassword.requestFocus()
                hasError = true
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden. Revísalas", Toast.LENGTH_SHORT).show()
                binding.etConfirmPassword.requestFocus()
                hasError = true
            }

            if (!hasError) {
                pendingName = name
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
        binding.btnRegister.text = "Creando tu cuenta..."
        lifecycleScope.launch {
            SupabaseService.registerUser(pendingEmail, pendingPassword)
                .onSuccess { userId ->
                    SupabaseService.createProfile(userId, pendingName)
                    Toast.makeText(this@RegisterActivity, "¡Tu cuenta está lista! Ya puedes ingresar", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
                .onFailure { error ->
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = getString(R.string.btn_register)
                    val msg = error.message ?: ""
                    val friendlyMsg = when {
                        msg.contains("already", true) || msg.contains("registered", true) ->
                            "Ese correo ya tiene una cuenta. ¿Querías ingresar?"
                        msg.contains("password", true) ->
                            "Hubo un problema con la contraseña. Asegúrate de que tenga al menos 6 caracteres"
                        else -> "No pudimos crear tu cuenta. Intenta de nuevo"
                    }
                    Toast.makeText(this@RegisterActivity, friendlyMsg, Toast.LENGTH_LONG).show()
                }
        }
    }
}
