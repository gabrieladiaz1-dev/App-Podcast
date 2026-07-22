package com.example.audify

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseService {


    // ── Credenciales de Supabase ──
    private const val SUPABASE_URL = "https://oaayubturfjbtiuhvxpk.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9hYXl1YnR1cmZqYnRpdWh2eHBrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2NTA1NjEsImV4cCI6MjEwMDIyNjU2MX0.F15ycjiRUHxddihRr78fMvroxKmdhkJAlBcTz5huxz0"
    // ────────────────────────────

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            httpEngine = OkHttp.create()
        }
    }

    // ──── Parte del registro  ────

    suspend fun registerUser(
        email: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentUserOrNull()?.id
                ?: error("No se pudo obtener el ID del usuario")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProfile(userId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["profiles"].insert(
                mapOf("id" to userId, "name" to name)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── perfil del usuario ────

    data class Profile(
        val id: String = "",
        val name: String = "",
        val avatar_url: String? = null,
        val created_at: String? = null
    )

    suspend fun getCurrentUserEmail(): String? {
        return client.auth.currentUserOrNull()?.email
    }

    suspend fun getProfile(): Profile = withContext(Dispatchers.IO) {
        val user = client.auth.currentUserOrNull() ?: error("Usuario no autenticado")
        val defaultName = user.email?.substringBefore("@") ?: "Usuario"
        try {
            val profiles = client.postgrest["profiles"]
                .select {
                    filter { eq("id", user.id) }
                }
                .decodeList<Profile>()
            val existing = profiles.firstOrNull()
            if (existing != null && existing.name.isNotEmpty()) return@withContext existing
            if (existing != null) {
                client.postgrest["profiles"].update(
                    mapOf("name" to defaultName)
                ) { filter { eq("id", user.id) } }
            } else {
                client.postgrest["profiles"].insert(
                    mapOf("id" to user.id, "name" to defaultName)
                )
            }
            Profile(id = user.id, name = defaultName)
        } catch (e: Exception) {
            Profile(id = user.id, name = defaultName)
        }
    }

    suspend fun updateProfileName(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: error("Usuario no autenticado")
            client.postgrest["profiles"].update(
                mapOf("name" to name)
            ) {
                filter {
                    eq("id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── el Login ────

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            getProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── podcast aprovados ────

    data class Podcast(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val audio_url: String = "",
        val approved: Boolean = false
    )

    suspend fun getApprovedPodcasts(): Result<List<Podcast>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["podcasts"]
                .select {
                    filter {
                        eq("approved", true)
                    }
                }
                .decodeList<Podcast>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── ffavoritos ────

    data class Favorite(
        val user_id: String = "",
        val podcast_id: String = "",
        val created_at: String = ""
    )

    suspend fun addFavorite(favorite: Favorite): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["favorites"].insert(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── subir audios al priv" ────

    suspend fun uploadAudio(
        bucketName: String = "priv",
        path: String,
        audioBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = client.storage.from(bucketName).upload(path, audioBytes)
            Result.success(result.path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper para leer un Uri como ByteArray
    fun readUriToBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    // ──── categories test connection  ────

    data class Category(
        val id: Int = 0,
        val name: String = "",
        val created_at: String = ""
    )

    suspend fun addCategory(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["categories"].insert(mapOf("name" to name))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["categories"].select().decodeList<Category>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──── url bucket pod  ────

    fun getPublicAudioUrl(path: String): String {
        return client.storage.from("pod").publicUrl(path)
    }
}
