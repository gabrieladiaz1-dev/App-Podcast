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
import io.github.jan.supabase.serializer.JacksonSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SupabaseService {

    private const val SUPABASE_URL = "https://oaayubturfjbtiuhvxpk.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9hYXl1YnR1cmZqYnRpdWh2eHBrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2NTA1NjEsImV4cCI6MjEwMDIyNjU2MX0.F15ycjiRUHxddihRr78fMvroxKmdhkJAlBcTz5huxz0"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            defaultSerializer = JacksonSerializer()
            install(Auth)
            install(Postgrest)
            install(Storage)
            httpEngine = OkHttp.create()
        }
    }

    fun preload() {
        scope.launch {
            try {
                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    tryToRefreshSession(session.refreshToken)
                    Log.d("SupabaseService", "Sesión restaurada y refrescada")
                }
            } catch (e: Exception) {
                Log.e("SupabaseService", "Error restaurando sesión: ${e.message}")
            }
        }
    }

    private suspend fun tryToRefreshSession(refreshToken: String) {
        try {
            client.auth.refreshSession(refreshToken)
        } catch (e: Exception) {
            Log.e("SupabaseService", "No se pudo refrescar: ${e.message}")
        }
    }

    suspend fun ensureValidSession(): Boolean {
        return try {
            val session = client.auth.currentSessionOrNull() ?: return false
            tryToRefreshSession(session.refreshToken)
            client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error en ensureValidSession: ${e.message}")
            false
        }
    }

    fun isUserLoggedIn(): Boolean {
        return try {
            client.auth.currentSessionOrNull() != null
        } catch (_: Exception) {
            false
        }
    }

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

    data class Profile(
        val id: String = "",
        val name: String = "",
        val avatar_url: String? = null,
        val created_at: String? = null
    )

    suspend fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentUserOrNull()?.email
        } catch (_: Exception) {
            null
        }
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

    suspend fun getProfileByUserId(userId: String): Profile? = withContext(Dispatchers.IO) {
        try {
            val profiles = client.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeList<Profile>()
            profiles.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfileName(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: error("Usuario no autenticado")
            client.postgrest["profiles"].update(
                mapOf("name" to name)
            ) {
                filter { eq("id", userId) }
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

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            client.auth.signInWith(Email) {
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

    data class Podcast(
        val id: String = "",
        val user_id: String = "",
        val title: String = "",
        val description: String = "",
        val category_id: String = "",
        val audio_url: String = "",
        val cover_url: String? = null,
        val approved: Boolean = false,
        val created_at: String = ""
    )

    suspend fun getApprovedPodcasts(): Result<List<Podcast>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["podcasts"]
                .select {
                    filter { eq("approved", true) }
                }
                .decodeList<Podcast>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getPodcastsByUserId(userId: String): Result<List<Podcast>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["podcasts"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<Podcast>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun insertPodcast(
        userId: String,
        title: String,
        description: String,
        categoryId: Long,
        audioUrl: String,
        coverUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = mutableMapOf<String, Any>(
                "user_id" to userId,
                "title" to title,
                "description" to description,
                "audio_url" to audioUrl,
                "approved" to false
            )
            if (categoryId > 0) record["category_id"] = categoryId
            if (!coverUrl.isNullOrEmpty()) record["cover_url"] = coverUrl
            client.postgrest["podcasts"].insert(record)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun uploadCoverImage(
        path: String,
        imageBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = client.storage.from("portadas").upload(path, imageBytes)
            Result.success(result.path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPublicCoverUrl(path: String): String {
        return client.storage.from("portadas").publicUrl(path)
    }

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
            Result.failure(wrapJwtError(e))
        }
    }

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

    fun readUriToBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    data class Category(
        val id: Long = 0,
        val name: String = ""
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
            Log.d("SupabaseService", "Fetching categories from Supabase...")
            val result = client.postgrest["categories"].select().decodeList<Category>()
            Log.d("SupabaseService", "Categories loaded: ${result.size} items")
            for (cat in result) {
                Log.d("SupabaseService", "  Category: id=${cat.id}, name=${cat.name}")
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching categories: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    class SessionExpiredException : Exception("Tu sesión expiró, por favor inicia sesión de nuevo")

    private fun isJwtError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("jwt") || msg.contains("expired") || msg.contains("401") || msg.contains("unauthorized")
    }

    private fun wrapJwtError(e: Exception): Exception =
        if (isJwtError(e)) SessionExpiredException() else e

    fun getPublicAudioUrl(bucketName: String = "pod", path: String): String {
        return client.storage.from(bucketName).publicUrl(path)
    }
}
