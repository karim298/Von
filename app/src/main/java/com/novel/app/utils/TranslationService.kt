package com.novel.app.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

object TranslationService {
    private val client = OkHttpClient()

    @Throws(Exception::class)
    fun translateToArabic(text: String): String {
        if (text.isEmpty()) return text
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=zh-CN&tl=ar&dt=t&q=${URLEncoder.encode(text, "UTF-8")}"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: return text
        val array = JSONArray(json)
        val sentences = array.getJSONArray(0)
        val result = StringBuilder()
        for (i in 0 until sentences.length()) {
            result.append(sentences.getJSONArray(i).getString(0))
        }
        return result.toString()
    }
}
