package com.example.audify

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.audify.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                R.id.nav_favoritos -> navController.navigate(R.id.favoritesFragment)
                R.id.nav_listas -> navController.navigate(R.id.listsFragment)
                R.id.nav_borradores -> Toast.makeText(this, R.string.drawer_borradores, Toast.LENGTH_SHORT).show()
                R.id.nav_cerrar_sesion -> {
                    lifecycleScope.launch {
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

        lifecycleScope.launch {
            try {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val profile = SupabaseService.getProfile()
                val name = profile.name.ifEmpty { email.substringBefore("@").ifEmpty { "Usuario" } }
                txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
                txtNombre.text = name
                txtCorreo.text = email
            } catch (e: Exception) {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val fallback = email.substringBefore("@").ifEmpty { "Usuario" }
                txtAvatar.text = fallback.firstOrNull()?.uppercase() ?: "?"
                txtNombre.text = fallback
                txtCorreo.text = email
            }
        }
    }
}
