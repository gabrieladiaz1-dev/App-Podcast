package com.example.audify

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

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

    suspend fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
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

    data class PodcastSupabase(
        @com.fasterxml.jackson.annotation.JsonProperty("id") val id: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("user_id") val user_id: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("title") val title: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("description") val description: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("category_id") val category_id: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("audio_url") val audio_url: String = "",
        @com.fasterxml.jackson.annotation.JsonProperty("cover_url") val cover_url: String? = null,
        @com.fasterxml.jackson.annotation.JsonProperty("approved") val approved: Boolean = false,
        @com.fasterxml.jackson.annotation.JsonProperty("created_at") val created_at: String = ""
    )

    private fun PodcastSupabase.toModel(): com.example.audify.model.Podcast {
        return com.example.audify.model.Podcast(
            id = this.id.hashCode(),
            supabaseId = this.id,
            title = this.title,
            author = "",
            description = this.description,
            duration = "",
            category = "",
            audioUrl = this.audio_url,
            coverUrl = this.cover_url,
            userId = this.user_id,
            approved = this.approved
        )
    }

    suspend fun getAllPodcasts(): Result<List<com.example.audify.model.Podcast>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["podcasts"]
                .select { filter { eq("approved", true) } }
                .decodeList<PodcastSupabase>()
            Log.d("SupabaseService", "getAllPodcasts: ${result.size} podcasts from Supabase")
            result.forEach { ps ->
                Log.d("SupabaseService", "  '${ps.title}' approved=${ps.approved} audio_url=${ps.audio_url}")
            }
            val mapped = result.map { ps ->
                val profile = getProfileByUserId(ps.user_id)
                val resolvedCover = resolveCoverUrl(ps.cover_url)
                ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover)
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getUserPodcasts(): Result<List<com.example.audify.model.Podcast>> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("No autenticado"))
            val result = client.postgrest["podcasts"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<PodcastSupabase>()
            val profile = getProfileByUserId(userId)
            val mapped = result.map { ps ->
                val resolvedCover = resolveCoverUrl(ps.cover_url)
                ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover)
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getPodcastByIntId(podcastId: Int): com.example.audify.model.Podcast? = withContext(Dispatchers.IO) {
        try {
            val all = client.postgrest["podcasts"]
                .select()
                .decodeList<PodcastSupabase>()
            Log.d("SupabaseService", "getPodcastByIntId: ${all.size} podcasts, looking for hashCode=$podcastId")
            all.forEach { ps ->
                Log.d("SupabaseService", "  podcast '${ps.title}' id=${ps.id} hashCode=${ps.id.hashCode()} approved=${ps.approved}")
            }
            val ps = all.find { it.id.hashCode() == podcastId } ?: return@withContext null
            Log.d("SupabaseService", "Found podcast '${ps.title}' approved=${ps.approved} audio_url=${ps.audio_url}")
            val profile = getProfileByUserId(ps.user_id)
            val resolvedCover = resolveCoverUrl(ps.cover_url)
            ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover)
        } catch (e: Exception) {
            Log.e("SupabaseService", "getPodcastByIntId error: ${e.message}", e)
            null
        }
    }

    suspend fun getPodcastsByUser(userId: String): Result<List<com.example.audify.model.Podcast>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["podcasts"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("approved", true)
                    }
                }
                .decodeList<PodcastSupabase>()
            val profile = getProfileByUserId(userId)
            val mapped = result.map { ps ->
                val resolvedCover = resolveCoverUrl(ps.cover_url)
                ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover)
            }
            Result.success(mapped)
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
            val bucket = client.storage.from("portadas")
            try { bucket.delete(path) } catch (_: Exception) {}
            val result = bucket.upload(path, imageBytes, upsert = false)
            Result.success(result.path)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    fun getPublicCoverUrl(path: String): String {
        return client.storage.from("portadas").publicUrl(path)
    }

    data class Favorite(
        val id: String? = null,
        val user_id: String = "",
        val podcast_id: String = "",
        val created_at: String? = null
    )

    suspend fun addFavorite(favorite: Favorite): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["favorites"].insert(favorite)
            Log.d("SupabaseService", "Favorito agregado: user=${favorite.user_id} podcast=${favorite.podcast_id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error agregando favorito: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun removeFavorite(userId: String, podcastId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["favorites"].delete {
                filter {
                    eq("user_id", userId)
                    eq("podcast_id", podcastId)
                }
            }
            Log.d("SupabaseService", "Favorito eliminado: user=$userId podcast=$podcastId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error eliminando favorito: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun isFavorited(userId: String, podcastId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["favorites"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("podcast_id", podcastId)
                    }
                }
                .decodeList<Favorite>()
            Log.d("SupabaseService", "isFavorited check: user=$userId podcast=$podcastId result=${result.isNotEmpty()}")
            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error consultando favorito: ${e.message}", e)
            false
        }
    }

    suspend fun getFavoritePodcasts(userId: String): Result<List<com.example.audify.model.Podcast>> = withContext(Dispatchers.IO) {
        try {
            val favs = client.postgrest["favorites"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<Favorite>()
            Log.d("SupabaseService", "getFavoritePodcasts: user=$userId found ${favs.size} favorites")
            if (favs.isEmpty()) return@withContext Result.success(emptyList())
            val podcasts = mutableListOf<com.example.audify.model.Podcast>()
            for (fav in favs) {
                try {
                    Log.d("SupabaseService", "Buscando podcast id=${fav.podcast_id}")
                    val list = client.postgrest["podcasts"]
                        .select {
                            filter {
                                eq("id", fav.podcast_id)
                                eq("approved", true)
                            }
                        }
                        .decodeList<PodcastSupabase>()
                    Log.d("SupabaseService", "Podcast id=${fav.podcast_id} encontrado: ${list.isNotEmpty()}")
                    if (list.isNotEmpty()) {
                        val ps = list.first()
                        val profile = getProfileByUserId(ps.user_id)
                        val resolvedCover = resolveCoverUrl(ps.cover_url)
                        podcasts.add(ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover))
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseService", "Error cargando podcast favorito ${fav.podcast_id}: ${e.message}")
                }
            }
            Log.d("SupabaseService", "Total favoritos cargados: ${podcasts.size}")
            Result.success(podcasts)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error en getFavoritePodcasts: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun uploadAudio(
        bucketName: String = "priv",
        path: String,
        audioBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val session = client.auth.currentSessionOrNull()
            if (session == null) {
                return@withContext Result.failure(SessionExpiredException())
            }
            val bucket = client.storage.from(bucketName)
            try { bucket.delete(path) } catch (_: Exception) {}
            val result = bucket.upload(path, audioBytes, upsert = false)
            Result.success(result.path)
        } catch (e: Exception) {
            Result.failure(wrapJwtError(e))
        }
    }

    fun readUriToBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
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
            val result = client.postgrest["categories"].select().decodeList<Category>()
            Result.success(result)
        } catch (e: Exception) {
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

    suspend fun createSignedUrl(bucketName: String, path: String, expiresInMinutes: Long = 60): String {
        return client.storage.from(bucketName)
            .createSignedUrl(path, expiresInMinutes.minutes)
    }

    fun extractStoragePath(url: String): Pair<String, String> {
        val marker = "/object/public/"
        val idx = url.indexOf(marker)
        if (idx == -1) {
            val markerAuth = "/object/sign/"
            val idxAuth = url.indexOf(markerAuth)
            if (idxAuth == -1) return "priv" to url
            val after = url.substring(idxAuth + markerAuth.length)
            val queryIdx = after.indexOf('?')
            val withoutQuery = if (queryIdx != -1) after.substring(0, queryIdx) else after
            val slashIdx = withoutQuery.indexOf('/')
            val bucket = withoutQuery.substring(0, slashIdx)
            val path = withoutQuery.substring(slashIdx + 1)
            return bucket to path
        }
        val after = url.substring(idx + marker.length)
        val slashIdx = after.indexOf('/')
        val bucket = after.substring(0, slashIdx)
        val path = after.substring(slashIdx + 1)
        return bucket to path
    }

    suspend fun resolveAudioUrl(audioUrl: String, approved: Boolean): String? {
        if (audioUrl.isEmpty()) return null
        if (!audioUrl.startsWith("http")) return audioUrl

        val (originalBucket, path) = extractStoragePath(audioUrl)
        Log.d("SupabaseService", "resolveAudioUrl: approved=$approved bucket=$originalBucket path=$path")

        if (approved) {
            val buckets = listOf("pod", "priv")
            for (bucket in buckets) {
                try {
                    val signed = createSignedUrl(bucket, path)
                    Log.d("SupabaseService", "Audio signed URL OK: bucket=$bucket url=$signed")
                    return signed
                } catch (e: Exception) {
                    Log.d("SupabaseService", "Signed URL falló en bucket $bucket: ${e.message}")
                }
            }
            Log.e("SupabaseService", "No se pudo generar signed URL para ningún bucket")
            return null
        }

        val userId = try { client.auth.currentUserOrNull()?.id } catch (_: Exception) { null }
        if (userId == null) {
            Log.d("SupabaseService", "Audio pendiente sin usuario → null")
            return null
        }

        try {
            val signed = createSignedUrl("priv", path)
            Log.d("SupabaseService", "Audio signed URL priv (owner): $signed")
            return signed
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error signed URL priv: ${e.message}")
            return null
        }
    }

    suspend fun resolveCoverUrl(coverUrl: String?): String? {
        if (coverUrl.isNullOrEmpty()) return null
        if (!coverUrl.startsWith("http")) return coverUrl
        return try {
            val (_, path) = extractStoragePath(coverUrl)
            getPublicCoverUrl(path)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error cover URL pública: ${e.message}")
            try {
                val (bucket, path2) = extractStoragePath(coverUrl)
                createSignedUrl(bucket, path2)
            } catch (e2: Exception) {
                coverUrl
            }
        }
    }

    data class PlaylistSupabase(
        val id: String = "",
        val user_id: String = "",
        val name: String = "",
        val created_at: String? = null
    )

    data class PlaylistItem(
        val id: String? = null,
        val playlist_id: String = "",
        val podcast_id: String = "",
        val created_at: String? = null
    )

    suspend fun createPlaylist(name: String): Result<PlaylistSupabase> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("No autenticado"))
            val record = mapOf("user_id" to userId, "name" to name)
            val result = client.postgrest["playlists"]
                .insert(record) { select() }
                .decodeList<PlaylistSupabase>()
            val playlist = result.first()
            Log.d("SupabaseService", "Lista creada: ${playlist.id} - ${playlist.name}")
            Result.success(playlist)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error creando lista: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getUserPlaylists(): Result<List<PlaylistSupabase>> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("No autenticado"))
            val result = client.postgrest["playlists"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<PlaylistSupabase>()
            Log.d("SupabaseService", "Listas del usuario: ${result.size}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error cargando listas: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["playlist_items"].delete {
                filter { eq("playlist_id", playlistId) }
            }
            client.postgrest["playlists"].delete {
                filter { eq("id", playlistId) }
            }
            Log.d("SupabaseService", "Lista eliminada: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error eliminando lista: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getPlaylistItems(playlistId: String): Result<List<PlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["playlist_items"]
                .select { filter { eq("playlist_id", playlistId) } }
                .decodeList<PlaylistItem>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error cargando items de lista: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun addPodcastToPlaylist(playlistId: String, podcastId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = client.postgrest["playlist_items"]
                .select {
                    filter {
                        eq("playlist_id", playlistId)
                        eq("podcast_id", podcastId)
                    }
                }
                .decodeList<PlaylistItem>()
            if (existing.isNotEmpty()) return@withContext Result.success(Unit)

            val record = mapOf("playlist_id" to playlistId, "podcast_id" to podcastId)
            client.postgrest["playlist_items"].insert(record)
            Log.d("SupabaseService", "Podcast $podcastId agregado a lista $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error agregando a lista: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun removePodcastFromPlaylist(playlistId: String, podcastId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["playlist_items"].delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("podcast_id", podcastId)
                }
            }
            Log.d("SupabaseService", "Podcast $podcastId removido de lista $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error removiendo de lista: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }

    suspend fun getPlaylistPodcasts(playlistId: String): Result<List<com.example.audify.model.Podcast>> = withContext(Dispatchers.IO) {
        try {
            val items = getPlaylistItems(playlistId).getOrNull() ?: emptyList()
            if (items.isEmpty()) return@withContext Result.success(emptyList())
            val podcasts = mutableListOf<com.example.audify.model.Podcast>()
            for (item in items) {
                try {
                    val list = client.postgrest["podcasts"]
                        .select {
                            filter {
                                eq("id", item.podcast_id)
                                eq("approved", true)
                            }
                        }
                        .decodeList<PodcastSupabase>()
                    if (list.isNotEmpty()) {
                        val ps = list.first()
                        val profile = getProfileByUserId(ps.user_id)
                        val resolvedCover = resolveCoverUrl(ps.cover_url)
                        podcasts.add(ps.toModel().copy(author = profile?.name ?: "Desconocido", coverUrl = resolvedCover))
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseService", "Error cargando podcast de lista ${item.podcast_id}: ${e.message}")
                }
            }
            Result.success(podcasts)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error en getPlaylistPodcasts: ${e.message}", e)
            Result.failure(wrapJwtError(e))
        }
    }
}
