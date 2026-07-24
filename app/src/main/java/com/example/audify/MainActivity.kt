package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.audify.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.init(this)
        SupabaseService.preload()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        loadDrawerUserData()

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> navController.navigate(R.id.inicioFragment)
                R.id.nav_subir -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.uploadFragment)
                    }
                }
                R.id.nav_favoritos -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.favoritesFragment)
                    }
                }
                R.id.nav_listas -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.listsFragment)
                    }
                }
                R.id.nav_borradores -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.draftsFragment)
                    }
                }
                R.id.nav_cerrar_sesion -> {
                    SessionManager.clearSession()
                    lifecycleScope.launch(Dispatchers.IO) {
                        SupabaseService.signOut()
                    }
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadDrawerUserData() {
        val headerView = binding.navigationView.getHeaderView(0)
        val txtAvatar = headerView.findViewById<TextView>(R.id.txtDrawerAvatar)
        val txtNombre = headerView.findViewById<TextView>(R.id.txtDrawerNombre)
        val txtCorreo = headerView.findViewById<TextView>(R.id.txtDrawerCorreo)

        if (!SessionManager.isLoggedIn()) {
            txtAvatar.text = "?"
            txtNombre.text = "Invitado"
            txtCorreo.text = "inicia sesión para ver tu perfil"
            return
        }

        lifecycleScope.launch {
            val email = withContext(Dispatchers.IO) {
                SupabaseService.getCurrentUserEmail() ?: ""
            }
            val profile = withContext(Dispatchers.IO) {
                try { SupabaseService.getProfile() } catch (_: Exception) {
                    SupabaseService.Profile(name = email.substringBefore("@"))
                }
            }
            val name = profile.name.ifEmpty { email.substringBefore("@").ifEmpty { "Usuario" } }
            txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
            txtNombre.text = name
            txtCorreo.text = email
        }
    }
}
