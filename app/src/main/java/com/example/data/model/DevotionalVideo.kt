package com.example.data.model

data class DevotionalVideo(
    val videoId: String,
    val title: String,
    val author: String,
    val authorId: String,
    val thumbnailUrl: String,
    val lengthSeconds: Int,
    val durationText: String,
    val isVerified: Boolean = false
) {
    val streamUrlPlaceholder: String
        get() = "/latest_version?id=$videoId&itag=140"
}
