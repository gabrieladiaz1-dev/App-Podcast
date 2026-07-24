package com.example.audify.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DraftsManager {

    private const val PREFS_NAME = "audify_drafts"
    private const val KEY_DRAFTS = "drafts_json"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    data class Draft(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val audioFilePath: String,
        val audioFileName: String,
        val coverFilePath: String? = null,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun saveDraft(context: Context, draft: Draft) {
        val drafts = getAllDrafts().toMutableList()
        drafts.add(0, draft)
        val jsonArray = JSONArray()
        drafts.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("description", d.description)
                put("category", d.category)
                put("audioFilePath", d.audioFilePath)
                put("audioFileName", d.audioFileName)
                put("coverFilePath", d.coverFilePath ?: "")
                put("createdAt", d.createdAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_DRAFTS, jsonArray.toString()).apply()
    }

    fun getAllDrafts(): List<Draft> {
        val json = prefs.getString(KEY_DRAFTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val drafts = mutableListOf<Draft>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                drafts.add(
                    Draft(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        category = obj.getString("category"),
                        audioFilePath = obj.getString("audioFilePath"),
                        audioFileName = obj.getString("audioFileName"),
                        coverFilePath = obj.optString("coverFilePath", "").ifEmpty { null },
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
            drafts
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteDraft(context: Context, draftId: String) {
        val drafts = getAllDrafts().toMutableList()
        val draft = drafts.find { it.id == draftId }
        draft?.let {
            try {
                File(it.audioFilePath).delete()
                it.coverFilePath?.let { path -> File(path).delete() }
            } catch (_: Exception) {}
        }
        drafts.removeAll { it.id == draftId }
        val jsonArray = JSONArray()
        drafts.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("description", d.description)
                put("category", d.category)
                put("audioFilePath", d.audioFilePath)
                put("audioFileName", d.audioFileName)
                put("coverFilePath", d.coverFilePath ?: "")
                put("createdAt", d.createdAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_DRAFTS, jsonArray.toString()).apply()
    }

    fun loadDraft(draftId: String): Draft? {
        return getAllDrafts().find { it.id == draftId }
    }
}
