package com.novel.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.novel.app.R
import com.novel.app.models.Chapter
import com.novel.app.network.ApiClient
import com.novel.app.utils.EpubGenerator
import com.novel.app.utils.TextCleaner
import okhttp3.OkHttpClient
import okhttp3.Request

class DownloadActivity : AppCompatActivity() {
    companion object {
        fun newIntent(
            context: Context,
            articleId: String,
            title: String,
            author: String,
            intro: String,
            chapters: List<Chapter>
        ): Intent {
            val intent = Intent(context, DownloadActivity::class.java)
            intent.putExtra("article_id", articleId)
            intent.putExtra("title", title)
            intent.putExtra("author", author)
            intent.putExtra("intro", intro)
            intent.putExtra("chapters", ArrayList(chapters))
            return intent
        }
    }

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercent: TextView
    private lateinit var btnStart: Button

    private var articleId = ""
    private var title = ""
    private var author = ""
    private var intro = ""
    private var chapters = listOf<Chapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        articleId = intent.getStringExtra("article_id") ?: ""
        title = intent.getStringExtra("title") ?: ""
        author = intent.getStringExtra("author") ?: ""
        intro = intent.getStringExtra("intro") ?: ""
        @Suppress("UNCHECKED_CAST")
        chapters = intent.getSerializableExtra("chapters") as? List<Chapter> ?: listOf()

        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        progressPercent = findViewById(R.id.progressPercent)
        btnStart = findViewById(R.id.btnStartDownload)

        if (chapters.isEmpty()) {
            statusText.text = "لا توجد فصول"
            btnStart.isEnabled = false
        } else {
            statusText.text = "عدد الفصول: ${chapters.size}"
            btnStart.setOnClickListener {
                DownloadTask().execute()
            }
        }
    }

    inner class DownloadTask : AsyncTask<Void, Int, Uri?>() {
        private var error: String? = null

        override fun onPreExecute() {
            btnStart.isEnabled = false
            statusText.text = "جاري التحميل..."
            progressBar.max = chapters.size
            progressBar.progress = 0
            progressPercent.text = "0%"
        }

        override fun doInBackground(vararg params: Void): Uri? {
            return try {
                var coverBytes: ByteArray? = null
                try {
                    val coverUrl = ApiClient.getCoverUrl(articleId)
                    val client = OkHttpClient()
                    val req = Request.Builder().url(coverUrl).build()
                    client.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            coverBytes = response.body?.bytes()
                        }
                    }
                } catch (_: Exception) {
                }

                val chapterData = mutableListOf<Map<String, String>>()
                for ((index, ch) in chapters.withIndex()) {
                    val content = ApiClient.getChapterContent(articleId, ch.id)
                    val extracted = TextCleaner.extractTitleFromFirstLine(content)
                    val name = extracted?.first ?: ch.name
                    val cleanContent = extracted?.second ?: content
                    chapterData.add(mapOf("name" to name, "content" to cleanContent))
                    publishProgress(index + 1)
                }

                EpubGenerator(this@DownloadActivity).generate(
                    title,
                    author,
                    intro,
                    chapterData,
                    coverBytes
                )
            } catch (e: Exception) {
                error = e.message
                null
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            val progress = values[0] ?: 0
            progressBar.progress = progress
            val percent = (progress.toDouble() / chapters.size * 100).toInt()
            progressPercent.text = "$percent%"
            statusText.text = "تم تحميل $progress من ${chapters.size}"
        }

        override fun onPostExecute(result: Uri?) {
            btnStart.isEnabled = true
            if (result != null) {
                statusText.text = "✅ تم الحفظ في: $result"
                progressPercent.text = "100%"
                Toast.makeText(this@DownloadActivity, "تم الحفظ بنجاح", Toast.LENGTH_LONG).show()
            } else {
                statusText.text = "❌ فشل التحميل: ${error ?: "خطأ غير معروف"}"
                Toast.makeText(this@DownloadActivity, "فشل التحميل", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
