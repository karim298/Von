package com.novel.app.ui
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.novel.app.R
import com.novel.app.models.Chapter
import com.novel.app.models.Novel
import com.novel.app.network.ApiClient
import com.novel.app.utils.PreferencesService
import com.novel.app.utils.TranslationService

class DetailActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_MANAGE_STORAGE = 1001
        fun newIntent(context: Context, articleId: String, novel: Novel) =
            Intent(context, DetailActivity::class.java).putExtra("article_id", articleId).putExtra("novel", novel)
    }
    private lateinit var novel: Novel
    private var articleId = ""
    private var chapters = listOf<Chapter>()
    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var prefs: PreferencesService
    private var translationEnabled = false
    private lateinit var titleView: TextView
    private lateinit var introView: TextView
    private lateinit var btnDownload: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        prefs = PreferencesService(this)
        translationEnabled = prefs.isTranslationEnabled()
        articleId = intent.getStringExtra("article_id") ?: ""
        novel = intent.getSerializableExtra("novel") as? Novel ?: run { finish(); return }
        titleView = findViewById(R.id.detailTitle)
        introView = findViewById(R.id.detailIntro)
        btnDownload = findViewById(R.id.btnDownload)
        findViewById<TextView>(R.id.detailAuthor).text = "✍️ ${novel.author}"
        findViewById<TextView>(R.id.detailChaptersCount).text = "${novel.chaptersCount} فصل"
        titleView.text = novel.title
        introView.text = novel.intro
        Glide.with(this).load(ApiClient.getCoverUrl(articleId)).placeholder(R.drawable.ic_book_placeholder).error(R.drawable.ic_book_placeholder).into(findViewById(R.id.detailCover))
        chapterAdapter = ChapterAdapter()
        findViewById<RecyclerView>(R.id.chapterRecyclerView).apply { layoutManager = LinearLayoutManager(this@DetailActivity); adapter = chapterAdapter }
        LoadChaptersTask().execute(articleId)
        findViewById<Button>(R.id.btnRead).setOnClickListener { if (chapters.isNotEmpty()) ReaderActivity.start(this, articleId, chapters, novel.title) else Toast.makeText(this, "لا توجد فصول", Toast.LENGTH_SHORT).show() }
        btnDownload.setOnClickListener { checkStoragePermissionAndDownload() }
        if (translationEnabled && novel.title.isNotEmpty()) TranslateInfoTask().execute()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 1, if (translationEnabled) "إلغاء الترجمة" else "ترجمة").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            translationEnabled = !translationEnabled
            prefs.saveTranslationEnabled(translationEnabled)
            if (translationEnabled) TranslateInfoTask().execute()
            else { titleView.text = novel.title; introView.text = novel.intro; chapterAdapter.notifyDataSetChanged() }
            invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    inner class LoadChaptersTask : AsyncTask<String, Void, List<Chapter>?>() {
        override fun doInBackground(vararg params: String): List<Chapter>? = try { ApiClient.getChapterList(params[0]) } catch (e: Exception) { null }
        override fun onPostExecute(result: List<Chapter>?) { if (result != null && result.isNotEmpty()) { chapters = result; chapterAdapter.submitList(chapters) } else Toast.makeText(this@DetailActivity, "فشل تحميل الفصول", Toast.LENGTH_SHORT).show() }
    }
    inner class TranslateInfoTask : AsyncTask<Void, Void, Pair<String, String>?>() {
        override fun doInBackground(vararg params: Void): Pair<String, String>? = try { Pair(TranslationService.translateToArabic(novel.title), if (novel.intro.isNotEmpty()) TranslationService.translateToArabic(novel.intro) else "") } catch (e: Exception) { null }
        override fun onPostExecute(result: Pair<String, String>?) { if (result != null) { titleView.text = result.first; if (result.second.isNotEmpty()) introView.text = result.second } else Toast.makeText(this@DetailActivity, "فشل الترجمة", Toast.LENGTH_SHORT).show() }
    }
    inner class ChapterAdapter : androidx.recyclerview.widget.ListAdapter<Chapter, ChapterAdapter.ChapterViewHolder>(object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Chapter>() { override fun areItemsTheSame(old: Chapter, new: Chapter) = old.id == new.id; override fun areContentsTheSame(old: Chapter, new: Chapter) = old == new }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChapterViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) { holder.textView.text = "${position+1}. ${getItem(position).name}" }
        inner class ChapterViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) { val textView: TextView = view.findViewById(android.R.id.text1) }
    }
    private fun checkStoragePermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { if (!android.os.Environment.isExternalStorageManager()) startActivityForResult(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(android.net.Uri.fromParts("package", packageName, null)), REQUEST_MANAGE_STORAGE) else startDownloadActivity() }
        else { if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_MANAGE_STORAGE) else startDownloadActivity() }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == REQUEST_MANAGE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()) startDownloadActivity() else Toast.makeText(this, "لم يتم منح الإذن", Toast.LENGTH_SHORT).show() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == REQUEST_MANAGE_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startDownloadActivity() else Toast.makeText(this, "تم رفض الإذن", Toast.LENGTH_SHORT).show() }
    private fun startDownloadActivity() { startActivity(DownloadActivity.newIntent(this, articleId, novel.title, novel.author, novel.intro, chapters)) }
}
