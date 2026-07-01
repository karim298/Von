package com.novel.app.ui
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novel.app.R
import com.novel.app.models.Novel
import com.novel.app.network.ApiClient
import com.novel.app.utils.PreferencesService
import com.novel.app.utils.TranslationService

class SearchActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NovelAdapter
    private lateinit var prefs: PreferencesService
    private var translationEnabled = false
    private val translatedTitles = mutableMapOf<String, String>()
    private val translatedIntros = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        prefs = PreferencesService(this)
        translationEnabled = prefs.isTranslationEnabled()
        searchInput = findViewById(R.id.searchInput)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NovelAdapter { startActivity(DetailActivity.newIntent(this, it.articleId, it)) }
        recyclerView.adapter = adapter
        findViewById<Button>(R.id.searchBtn).setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) SearchTask().execute(query)
            else Toast.makeText(this, "أدخل كلمة بحث", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 1, if (translationEnabled) "إلغاء الترجمة" else "ترجمة").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            translationEnabled = !translationEnabled
            prefs.saveTranslationEnabled(translationEnabled)
            if (translationEnabled && adapter.itemCount > 0) TranslateResultsTask().execute()
            else { translatedTitles.clear(); translatedIntros.clear(); adapter.notifyDataSetChanged() }
            invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class SearchTask : AsyncTask<String, Void, List<Novel>?>() {
        override fun doInBackground(vararg params: String): List<Novel>? = try { ApiClient.searchNovels(params[0], 1) } catch (e: Exception) { null }
        override fun onPostExecute(result: List<Novel>?) {
            if (result == null) Toast.makeText(this@SearchActivity, "فشل البحث", Toast.LENGTH_SHORT).show()
            else { adapter.submitList(result); if (translationEnabled) TranslateResultsTask().execute() }
        }
    }

    inner class TranslateResultsTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            val list = adapter.currentList ?: return null
            try {
                for (novel in list) {
                    if (!translatedTitles.containsKey(novel.articleId))
                        translatedTitles[novel.articleId] = TranslationService.translateToArabic(novel.title)
                    if (novel.intro.isNotEmpty() && !translatedIntros.containsKey(novel.articleId))
                        translatedIntros[novel.articleId] = TranslationService.translateToArabic(novel.intro)
                }
            } catch (e: Exception) { e.printStackTrace() }
            return null
        }
        override fun onPostExecute(result: Void?) { adapter.notifyDataSetChanged() }
    }

    inner class NovelAdapter(private val onItemClick: (Novel) -> Unit) :
        androidx.recyclerview.widget.ListAdapter<Novel, NovelAdapter.NovelViewHolder>(
            object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Novel>() {
                override fun areItemsTheSame(oldItem: Novel, newItem: Novel) = oldItem.articleId == newItem.articleId
                override fun areContentsTheSame(oldItem: Novel, newItem: Novel) = oldItem == newItem
            }) {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NovelViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_novel, parent, false)
            return NovelViewHolder(view, onItemClick)
        }
        override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
            val novel = getItem(position)
            val title = if (translationEnabled && translatedTitles.containsKey(novel.articleId)) translatedTitles[novel.articleId]!! else novel.title
            holder.bind(novel, title)
        }
        class NovelViewHolder(itemView: android.view.View, private val onItemClick: (Novel) -> Unit) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val cover: android.widget.ImageView = itemView.findViewById(R.id.coverImage)
            private val titleView: android.widget.TextView = itemView.findViewById(R.id.novelTitle)
            private val authorView: android.widget.TextView = itemView.findViewById(R.id.novelAuthor)
            private val chaptersView: android.widget.TextView = itemView.findViewById(R.id.novelChapters)
            fun bind(novel: Novel, title: String) {
                titleView.text = title
                authorView.text = "✍️ ${novel.author}"
                chaptersView.text = "📚 ${novel.chaptersCount} فصل"
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(ApiClient.getCoverUrl(novel.articleId))
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(cover)
                itemView.setOnClickListener { onItemClick(novel) }
            }
        }
    }
}
