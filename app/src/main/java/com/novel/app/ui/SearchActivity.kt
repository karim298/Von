package com.novel.app.ui

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

        adapter = NovelAdapter { novel ->
            startActivity(DetailActivity.newIntent(this, novel.articleId, novel))
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.searchBtn).setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                SearchTask().execute(query)
            } else {
                Toast.makeText(this, "أدخل كلمة بحث", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 1, if (translationEnabled) "إلغاء الترجمة" else "ترجمة")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            translationEnabled = !translationEnabled
            prefs.saveTranslationEnabled(translationEnabled)
            if (translationEnabled && adapter.itemCount > 0) {
                TranslateResultsTask().execute()
            } else {
                translatedTitles.clear()
                translatedIntros.clear()
                adapter.submitList(adapter.currentList)
            }
            invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class SearchTask : AsyncTask<String, Void, List<Novel>?>() {
        override fun doInBackground(vararg params: String): List<Novel>? {
            return try {
                ApiClient.searchNovels(params[0], 1)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: List<Novel>?) {
            if (result == null) {
                Toast.makeText(this@SearchActivity, "فشل البحث", Toast.LENGTH_SHORT).show()
            } else {
                adapter.submitList(result)
                if (translationEnabled) {
                    TranslateResultsTask().execute()
                }
            }
        }
    }

    inner class TranslateResultsTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            val list = adapter.currentList ?: return null
            try {
                for (novel in list) {
                    if (!translatedTitles.containsKey(novel.articleId)) {
                        translatedTitles[novel.articleId] = TranslationService.translateToArabic(novel.title)
                    }
                    if (novel.intro.isNotEmpty() && !translatedIntros.containsKey(novel.articleId)) {
                        translatedIntros[novel.articleId] = TranslationService.translateToArabic(novel.intro)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            adapter.submitList(adapter.currentList)
        }
    }

    // =============================================================
    // NovelAdapter - تعريفه داخل class SearchActivity
    // =============================================================
    inner class NovelAdapter(private val onItemClick: (Novel) -> Unit) :
        ListAdapter<Novel, NovelAdapter.NovelViewHolder>(NovelDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_novel, parent, false)
            return NovelViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
            val novel = getItem(position)
            val title = if (translationEnabled && translatedTitles.containsKey(novel.articleId)) {
                translatedTitles[novel.articleId]!!
            } else {
                novel.title
            }
            holder.bind(novel, title)
        }

        // ViewHolder الداخلي
        inner class NovelViewHolder(
            itemView: View,
            private val onItemClick: (Novel) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val cover: ImageView = itemView.findViewById(R.id.coverImage)
            private val titleView: TextView = itemView.findViewById(R.id.novelTitle)
            private val authorView: TextView = itemView.findViewById(R.id.novelAuthor)
            private val chaptersView: TextView = itemView.findViewById(R.id.novelChapters)

            fun bind(novel: Novel, title: String) {
                titleView.text = title
                authorView.text = "✍️ ${novel.author}"
                chaptersView.text = "📚 ${novel.chaptersCount} فصل"

                Glide.with(itemView.context)
                    .load(ApiClient.getCoverUrl(novel.articleId))
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(cover)

                itemView.setOnClickListener { onItemClick(novel) }
            }
        }

        // DiffUtil Callback
        class NovelDiffCallback : DiffUtil.ItemCallback<Novel>() {
            override fun areItemsTheSame(oldItem: Novel, newItem: Novel): Boolean {
                return oldItem.articleId == newItem.articleId
            }

            override fun areContentsTheSame(oldItem: Novel, newItem: Novel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
