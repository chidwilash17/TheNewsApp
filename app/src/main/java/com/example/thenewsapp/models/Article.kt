package com.example.thenewsapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
// import android.os.Parcelable
// import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "articles"
)
// @Parcelize // Uncomment if using Parcelable
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val author: String?,
    val content: String?,
    val description: String?,
    val publishedAt: String?,
    val source: Source?, // Make this nullable if a source can be null
    val title: String?,
    val url: String?,
    val urlToImage: String?
): Serializable // Or Parcelable