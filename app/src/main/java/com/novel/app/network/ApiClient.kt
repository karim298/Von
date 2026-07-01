package com.novel.app.network
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.novel.app.models.Chapter
import com.novel.app.models.Novel
import com.novel.app.utils.TextCleaner
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ApiClient {
    private const val BASE_URL = "https://novel.cooks.tw"
    private val client = OkHttpClient()
    private val gson = Gson()

    @Throws(IOException::class)
    fun searchNovels(keyword: String, page: Int): List<Novel> {
        val url = "$BASE_URL/api/novel/search?q=${java.net.URLEncoder.encode(keyword, "UTF-8")}&page=$page&limit=20&lang=zh-CN"
        val request = Request.Builder().url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Android 11; Mobile; rv:147.0)")
            .addHeader("Accept-Language", "zh-CN,zh-TW;q=0.9")
            .build()
        client.newCall(request).use { response ->
            if (!response.isSuccessful) throw IOException("فشل البحث")
            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val items = json.getAsJsonObject("data").getAsJsonArray("items")
            return items.map { gson.fromJson(it, Novel::class.java) }
        }
    }

    @Throws(IOException::class)
    fun getBookDetail(articleId: String): Novel {
        val url = "$BASE_URL/api/novel/detail/$articleId?lang=zh-CN"
        val request = Request.Builder().url(url).build()
        client.newCall(request).use { response ->
            if (!response.isSuccessful) throw IOException("فشل التفاصيل")
            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            return gson.fromJson(json.getAsJsonObject("data"), Novel::class.java)
        }
    }

    @Throws(IOException::class)
    fun getChapterList(articleId: String): List<Chapter> {
        val url = "$BASE_URL/api/chapter/list/$articleId?lang=zh-CN"
        val request = Request.Builder().url(url).build()
        client.newCall(request).use { response ->
            if (!response.isSuccessful) throw IOException("فشل قائمة الفصول")
            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val array = json.getAsJsonObject("data").asJsonArray
            return array.map { gson.fromJson(it, Chapter::class.java) }
        }
    }

    @Throws(IOException::class)
    fun getChapterContent(articleId: String, chapterId: String): String {
        val url = "$BASE_URL/api/chapter/content/$articleId/$chapterId?lang=zh-CN"
        val request = Request.Builder().url(url).build()
        client.newCall(request).use { response ->
            if (!response.isSuccessful) throw IOException("فشل المحتوى")
            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val raw = json.getAsJsonObject("data").get("content").asString
            return TextCleaner.clean(raw)
        }
    }

    fun getCoverUrl(articleId: String): String {
        return try {
            val num = articleId.replace("\\D".toRegex(), "").toInt()
            "https://pic.cooks.tw/${num / 1000}/$num/${num}s.jpg"
        } catch (e: Exception) { "" }
    }
}
