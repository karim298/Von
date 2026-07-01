package com.novel.app.utils
import android.content.Context
import android.content.SharedPreferences

class PreferencesService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("novel_prefs", Context.MODE_PRIVATE)
    fun saveTranslationEnabled(enabled: Boolean) { prefs.edit().putBoolean("enable_translation", enabled).apply() }
    fun isTranslationEnabled(): Boolean = prefs.getBoolean("enable_translation", false)
    fun saveLastChapter(articleId: String, index: Int) { prefs.edit().putInt("last_chapter_$articleId", index).apply() }
    fun getLastChapter(articleId: String): Int? = if (prefs.contains("last_chapter_$articleId")) prefs.getInt("last_chapter_$articleId", 0) else null
}
