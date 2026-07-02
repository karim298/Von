package com.novel.app.ui

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.novel.app.R
import com.novel.app.models.Chapter
import com.novel.app.network.ApiClient
import com.novel.app.utils.PreferencesService
import com.novel.app.utils.TextCleaner
import com.novel.app.utils.TranslationService

class ReaderActivity : AppCompatActivity() {
    companion object {
        fun start(context: Context, articleId: String, chapters: List<Chapter>, title: String) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("article_id", articleId)
            intent.putExtra("chapters", ArrayList(chapters))
            intent.putExtra("title", title)
            context.startActivity(intent)
        }
    }

    private lateinit var prefs: PreferencesService
    private lateinit var articleId: String
    private lateinit var chapters: List<Chapter>
    private var currentIndex = 0
    private lateinit var chapterTitle: TextView
    private lateinit var contentText: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnTranslate: Button
    private var originalContent = ""
    private var translationEnabled = false
    private var isTranslated = false
    private var isFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        prefs = PreferencesService(this)
        translationEnabled = prefs.isTranslationEnabled()

        articleId = intent.getStringExtra("article_id") ?: ""
        @Suppress("UNCHECKED_CAST")
        chapters = intent.getSerializableExtra("chapters") as? List<Chapter> ?: emptyList()

        if (chapters.isEmpty()) {
            Toast.makeText(this, "لا توجد فصول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chapterTitle = findViewById(R.id.chapterTitle)
        contentText = findViewById(R.id.contentText)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnTranslate = findViewById(R.id.btnTranslate)

        val startIndex = prefs.getLastChapter(articleId)?.takeIf { it in chapters.indices } ?: 0
        currentIndex = startIndex

        btnTranslate.setOnClickListener { toggleTranslation() }
        btnPrev.setOnClickListener { loadChapter(currentIndex - 1) }
        btnNext.setOnClickListener { loadChapter(currentIndex + 1) }
        contentText.setOnClickListener { toggleFullScreen() }

        loadChapter(currentIndex)
    }

    private fun loadChapter(index: Int) {
        if (index !in chapters.indices) return
        currentIndex = index
        val ch = chapters[index]
        chapterTitle.text = "${ch.name} (${index + 1}/${chapters.size})"
        isTranslated = false
        btnTranslate.text = if (translationEnabled) "ترجمة" else "ترجمة"
        LoadContentTask().execute(ch.id)
        prefs.saveLastChapter(articleId, index)
        btnPrev.isEnabled = currentIndex > 0
        btnNext.isEnabled = currentIndex < chapters.size - 1
    }

    private fun toggleTranslation() {
        if (originalContent.isEmpty()) return
        if (isTranslated) {
            contentText.text = originalContent
            isTranslated = false
            btnTranslate.text = "ترجمة"
        } else {
            TranslateTask().execute(originalContent)
        }
    }

    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        if (isFullScreen) {
            // إخفاء شريط الحالة وشريط التنقل
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars())
                    controller.hide(android.view.WindowInsets.Type.navigationBars())
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        } else {
            // إظهار شريط الحالة وشريط التنقل
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.show(android.view.WindowInsets.Type.statusBars())
                    controller.show(android.view.WindowInsets.Type.navigationBars())
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    inner class LoadContentTask : AsyncTask<String, Void, String?>() {
        override fun doInBackground(vararg params: String): String? {
            return try {
                ApiClient.getChapterContent(articleId, params[0])
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            if (result != null) {
                val clean = TextCleaner.extractTitleFromFirstLine(result)?.second ?: result
                originalContent = clean
                contentText.text = clean
                if (translationEnabled) {
                    TranslateTask().execute(clean)
                }
            } else {
                contentText.text = "فشل تحميل المحتوى"
            }
        }
    }

    inner class TranslateTask : AsyncTask<String, Void, String?>() {
        override fun onPreExecute() {
            btnTranslate.isEnabled = false
            btnTranslate.text = "جاري..."
        }

        override fun doInBackground(vararg params: String): String? {
            return try {
                TranslationService.translateToArabic(params[0])
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            btnTranslate.isEnabled = true
            if (!result.isNullOrEmpty()) {
                contentText.text = result
                isTranslated = true
                btnTranslate.text = "الأصلي"
            } else {
                Toast.makeText(this@ReaderActivity, "فشل الترجمة", Toast.LENGTH_SHORT).show()
                btnTranslate.text = "ترجمة"
            }
        }
    }
}
