package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_bhajans")
data class FavoriteBhajan(
    @PrimaryKey val videoId: String,
    val title: String,
    val author: String,
    val authorId: String,
    val thumbnailUrl: String,
    val lengthSeconds: Int,
    val durationText: String,
    val timestamp: Long = System.currentTimeMillis()
)
