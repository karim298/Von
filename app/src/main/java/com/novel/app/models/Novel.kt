package com.novel.app.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Novel(
    @SerializedName("articleid") val articleId: String = "",
    @SerializedName("articlename") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("intro") val intro: String = "",
    @SerializedName("chapters") val chaptersCount: Int = 0
) : Serializable
