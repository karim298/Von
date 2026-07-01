package com.novel.app.models
import com.google.gson.annotations.SerializedName
data class Chapter(
    @SerializedName(value = "chapterid", alternate = ["chapter_id", "id"])
    val id: String = "",
    @SerializedName(value = "chaptername", alternate = ["chapter_name", "title", "name"])
    val name: String = ""
)
