package com.example.audify

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.audify.databinding.ActivityMainBinding
import com.example.audify.service.AudioForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_NOTIFICATIONS_CODE = 301
    }

    lateinit var binding: ActivityMainBinding
    private var audioService: AudioForegroundService? = null
    private var isAudioServiceBound = false
    private val miniPlayerHandler = Handler(Looper.getMainLooper())
    private var lastMiniVisible = false
    private var lastMiniTitle = ""
    private var lastMiniIsPlaying = false
    private val miniPlayerUpdater = object : Runnable {
        override fun run() {
            refreshMiniPlayer()
            miniPlayerHandler.postDelayed(this, 900)
        }
    }

    private val audioConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioForegroundService.LocalBinder ?: return
            audioService = binder.getService()
            isAudioServiceBound = true
            refreshMiniPlayer()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isAudioServiceBound = false
            refreshMiniPlayer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.init(this)
        SupabaseService.preload()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        disableBottomNavActiveIndicator()
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            animateBottomNavSelection(item.itemId)
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
            if (item.itemId == R.id.inicioFragment) {
                builder.setPopUpTo(R.id.inicioFragment, true)
            } else {
                builder.setPopUpTo(R.id.inicioFragment, false)
            }
            try {
                navController.navigate(item.itemId, null, builder.build())
                true
            } catch (_: Exception) {
                false
            }
        }

        loadDrawerUserData()
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: android.view.View) {
                loadDrawerUserData()
            }
        })

        binding.navigationView.setNavigationItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .setLaunchSingleTop(true)
                .build()

            when (item.itemId) {
                R.id.nav_inicio -> navController.navigate(R.id.inicioFragment, null, navOptions)
                R.id.nav_subir -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.uploadFragment, null, navOptions)
                    }
                }
                R.id.nav_favoritos -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.favoritesFragment, null, navOptions)
                    }
                }
                R.id.nav_listas -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.listsFragment, null, navOptions)
                    }
                }
                R.id.nav_borradores -> {
                    if (!SessionManager.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        navController.navigate(R.id.draftsFragment, null, navOptions)
                    }
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        requestNotificationPermissionIfNeeded()

        binding.btnDrawerLogout.setOnClickListener {
            SessionManager.clearSession()
            lifecycleScope.launch(Dispatchers.IO) {
                SupabaseService.signOut()
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupMiniPlayerControls()
        refreshMiniPlayer()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS_CODE
        )
    }

    private fun setupMiniPlayerControls() {
        binding.btnMiniPlayPause.setOnClickListener {
            audioService?.togglePlayPause()
            refreshMiniPlayer()
        }
        binding.btnMiniStop.setOnClickListener {
            val stopIntent = Intent(this, AudioForegroundService::class.java).apply {
                action = AudioForegroundService.ACTION_STOP
            }
            startService(stopIntent)
            refreshMiniPlayer()
        }
        binding.miniPlayerContainer.setOnClickListener {
            val podcastId = audioService?.currentPlaybackPodcastId ?: -1
            if (podcastId <= 0) return@setOnClickListener
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            val currentPodcastId = navController.currentBackStackEntry
                ?.arguments
                ?.getInt("podcastId", -1) ?: -1
            if (navController.currentDestination?.id == R.id.detailFragment && currentPodcastId == podcastId) {
                return@setOnClickListener
            }
            val bundle = Bundle().apply { putInt("podcastId", podcastId) }
            try {
                navController.navigate(R.id.detailFragment, bundle)
            } catch (_: Exception) {
                // Ignore rapid re-entries.
            }
        }
    }

    private fun bindAudioServiceIfRunning() {
        if (isAudioServiceBound) return
        val intent = Intent(this, AudioForegroundService::class.java)
        try {
            bindService(intent, audioConnection, BIND_AUTO_CREATE)
        } catch (_: Exception) {
            // Ignore bind failures when service is not active.
        }
    }

    private fun unbindAudioServiceSafely() {
        if (!isAudioServiceBound) return
        try {
            unbindService(audioConnection)
        } catch (_: Exception) {
            // Ignore unbind races.
        }
        isAudioServiceBound = false
        audioService = null
    }

    private fun refreshMiniPlayer() {
        if (!isAudioServiceBound && AudioForegroundService.isServiceRunning) {
            bindAudioServiceIfRunning()
        }
        val svc = audioService
        val shouldShow = svc?.hasActivePlaybackSession == true
        if (!shouldShow) {
            if (lastMiniVisible) {
                binding.miniPlayerContainer.visibility = android.view.View.GONE
                lastMiniVisible = false
                lastMiniTitle = ""
            }
            return
        }

        val title = svc.currentPlaybackTitle.ifBlank { "Podcast en reproducción" }
        val isPlaying = svc.isPlaying
        if (!lastMiniVisible) {
            binding.miniPlayerContainer.visibility = android.view.View.VISIBLE
            lastMiniVisible = true
        }
        if (title != lastMiniTitle) {
            binding.txtMiniTitle.text = title
            lastMiniTitle = title
        }
        if (isPlaying != lastMiniIsPlaying) {
            binding.btnMiniPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.txtMiniNowPlaying.text = if (isPlaying) "Reproduciendo" else "Pausado"
            binding.btnMiniPlayPause.imageTintList = android.content.res.ColorStateList.valueOf(0xFF1E1B4B.toInt())
            binding.btnMiniStop.imageTintList = android.content.res.ColorStateList.valueOf(0xFF1E1B4B.toInt())
            lastMiniIsPlaying = isPlaying
        }
    }

    private fun animateBottomNavSelection(itemId: Int) {
        val itemView = binding.bottomNavigation.findViewById<android.view.View>(itemId) ?: return
        itemView.animate().cancel()
        itemView.scaleX = 0.94f
        itemView.scaleY = 0.94f
        itemView.alpha = 0.92f
        itemView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(180)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()
    }

    private fun disableBottomNavActiveIndicator() {
        try {
            val method = binding.bottomNavigation.javaClass
                .getMethod("setItemActiveIndicatorEnabled", Boolean::class.javaPrimitiveType)
            method.invoke(binding.bottomNavigation, false)
        } catch (_: Exception) {
            // Ignore when running with a Material version that does not expose this API.
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

        // Fast local fallback while remote profile/session is restored.
        val localEmail = SessionManager.getUserEmail().orEmpty()
        val localName = localEmail.substringBefore("@").ifEmpty { "Usuario" }
        txtAvatar.text = localName.firstOrNull()?.uppercase() ?: "?"
        txtNombre.text = localName
        txtCorreo.text = if (localEmail.isNotBlank()) localEmail else "Cargando..."

        lifecycleScope.launch {
            val email = withContext(Dispatchers.IO) {
                SupabaseService.getCurrentUserEmail() ?: localEmail
            }
            val profile = withContext(Dispatchers.IO) {
                try { SupabaseService.getProfile() } catch (_: Exception) {
                    SupabaseService.Profile(name = email.substringBefore("@"))
                }
            }
            val name = profile.name.ifEmpty { email.substringBefore("@").ifEmpty { "Usuario" } }
            txtAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
            txtNombre.text = name
            txtCorreo.text = if (email.isNotBlank()) email else localEmail
        }
    }

    override fun onResume() {
        super.onResume()
        loadDrawerUserData()
        bindAudioServiceIfRunning()
        miniPlayerHandler.removeCallbacks(miniPlayerUpdater)
        miniPlayerHandler.post(miniPlayerUpdater)
    }

    override fun onPause() {
        super.onPause()
        miniPlayerHandler.removeCallbacks(miniPlayerUpdater)
    }

    override fun onStop() {
        super.onStop()
        unbindAudioServiceSafely()
    }
}
