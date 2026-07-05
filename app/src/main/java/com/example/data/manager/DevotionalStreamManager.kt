package com.example.data.manager

import android.util.Log
import com.example.data.model.DevotionalVideo
import com.example.data.network.InvidiousApi
import com.example.data.network.VideoDetailsResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DevotionalStreamManager {

    companion object {
        private const val TAG = "DevotionalStream"

        // Rotating list of public Invidious instances for failover redundancy
        val PUBLIC_INSTANCES = listOf(
            "https://yewtu.be/",
            "https://invidious.projectsegfau.lt/",
            "https://invidious.privacydev.net/",
            "https://iv.ggtyler.dev/",
            "https://invidious.lunar.icu/",
            "https://iv.melmac.space/"
        )

        // Whitelisted ISKCON Channel IDs
        val ISKCON_CHANNEL_WHITELIST = setOf(
            "UC1t_sZ7X5fL6u_FshPebgXg", // ISKCON Desire Tree
            "UC1oR0YstC3N5Edfw7E0H5Mw", // 24 Hour Kirtan
            "UC_bK7N_3_66tL26uSefu2_g", // Mayapur TV / ISKCON Mayapur
            "UC9-gW_G73uN9-t208R9p23Q", // Lokanath Swami
            "UCO21V2eK3pM4XJ3u1Yg-7Lg", // Radhanath Swami
            "UC8X6X8zU9v1g_G_3o1F4Hhg", // Indradyumna Swami
            "UC864Ym02vM47p-0A7kK0C_g", // Kadamba Kanana Swami
            "UC_eW77pM0HofSOn8M-sW0cA", // Sivarama Swami
            "UCb8p6R6_U_fXqOqH6Xo3D9A"  // BB Govinda Swami
        )

        // Devotional Channel keywords to verify name-based ISKCON content
        val ISKCON_CHANNEL_KEYWORDS = listOf(
            "iskcon", "mayapur", "vrindavan", "kirtan", "bhajan", "prabhupada",
            "hare krishna", "hare krsna", "desire tree", "aindra", "madhava",
            "gauranga", "radhanath", "indradyumna", "lokanath", "sivarama",
            "kadamba kanana", "bhakti charu", "swami", "prabhu", "krsna",
            "krishna kirtan", "kartik", "damodar"
        )

        // Strict negative keywords to filter out non-devotional content
        val NEGATIVE_KEYWORDS = listOf(
            "official trailer", "official teaser", "movie song", "bollywood", "hollywood",
            "funny comedy", "gaming video", "unboxing", "tech review", "vlog", "mashup",
            "dj remix", "lofi remix", "tiktok", "reels", "hip hop", "rap", "pop music",
            "item song", "hot dance", "sexy", "news update", "politics", "trailer",
            "teaser", "unboxing", "tutorial", "commercial", "advertisement"
        )
    }

    private var activeInstanceIndex = 0

    val activeInstanceUrl: String
        get() = PUBLIC_INSTANCES[activeInstanceIndex]

    fun rotateInstance() {
        activeInstanceIndex = (activeInstanceIndex + 1) % PUBLIC_INSTANCES.size
        Log.d(TAG, "Rotating to new Invidious instance: ${activeInstanceUrl}")
    }

    private fun getApiClient(baseUrl: String): InvidiousApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(InvidiousApi::class.java)
    }

    /**
     * Requirement 3.2: Query Transformation
     * Expands the search query if it lacks devotional terms to maximize ISKCON kirtan results.
     */
    fun transformQuery(query: String): String {
        val lowercaseQuery = query.lowercase().trim()
        if (lowercaseQuery.isEmpty()) return "ISKCON Kirtan"

        // If user already specified "iskcon", "kirtan", "bhajan" etc., don't bloat it
        val hasDevotionalKeyword = ISKCON_CHANNEL_KEYWORDS.any { lowercaseQuery.contains(it) }
        return if (hasDevotionalKeyword) {
            query
        } else {
            "$query ISKCON Kirtan"
        }
    }

    /**
     * Requirement 3.3: Strict Negative Filtering
     * Returns true if the title is purely devotional and does not contain mainstream garbage tags.
     */
    fun isDevotionalTitle(title: String): Boolean {
        val titleLower = title.lowercase()
        return NEGATIVE_KEYWORDS.none { titleLower.contains(it) }
    }

    /**
     * Requirement 3.1: Whitelist checking
     * Checks if a channel is verified ISKCON channel or carries trusted names.
     */
    fun isVerifiedChannel(authorId: String?, authorName: String?): Boolean {
        if (authorId != null && ISKCON_CHANNEL_WHITELIST.contains(authorId)) {
            return true
        }
        if (authorName != null) {
            val nameLower = authorName.lowercase()
            return ISKCON_CHANNEL_KEYWORDS.any { nameLower.contains(it) }
        }
        return false
    }

    /**
     * Searches public Invidious instances with automatic rotation/failover redundancy.
     */
    suspend fun searchDevotionalTracks(userQuery: String): List<DevotionalVideo> {
        val expandedQuery = transformQuery(userQuery)
        Log.d(TAG, "Initiating search for raw query: '$userQuery' -> Expanded to: '$expandedQuery'")

        var attempts = 0
        val maxAttempts = PUBLIC_INSTANCES.size

        while (attempts < maxAttempts) {
            val currentBaseUrl = activeInstanceUrl
            try {
                val api = getApiClient(currentBaseUrl)
                val rawResults = api.searchVideos(expandedQuery)

                Log.d(TAG, "Search successful on $currentBaseUrl. Found ${rawResults.size} results.")

                // Process and filter results
                val filteredResults = rawResults.filter { item ->
                    item.type == "video" &&
                    !item.videoId.isNullOrEmpty() &&
                    !item.title.isNullOrEmpty() &&
                    isDevotionalTitle(item.title)
                }.map { item ->
                    val videoId = item.videoId!!
                    val title = item.title!!
                    val authorName = item.author ?: "Unknown Artist"
                    val authorId = item.authorId ?: ""
                    val lengthSeconds = item.lengthSeconds ?: 0

                    val minutes = lengthSeconds / 60
                    val seconds = lengthSeconds % 60
                    val durationText = String.format("%d:%02d", minutes, seconds)

                    // Find high/medium quality thumbnail
                    var thumbUrl = ""
                    val thumbnails = item.videoThumbnails
                    if (!thumbnails.isNullOrEmpty()) {
                        // Prefer standard or medium size
                        val bestThumb = thumbnails.find { it.quality == "medium" || it.quality == "standard" } 
                            ?: thumbnails.first()
                        thumbUrl = bestThumb.url ?: ""
                    }
                    if (thumbUrl.isEmpty()) {
                        thumbUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                    } else if (thumbUrl.startsWith("/")) {
                        // Relative URL from the Invidious instance
                        thumbUrl = currentBaseUrl.removeSuffix("/") + thumbUrl
                    }

                    val isVerified = isVerifiedChannel(authorId, authorName)

                    DevotionalVideo(
                        videoId = videoId,
                        title = title,
                        author = authorName,
                        authorId = authorId,
                        thumbnailUrl = thumbUrl,
                        lengthSeconds = lengthSeconds,
                        durationText = durationText,
                        isVerified = isVerified
                    )
                }

                // Requirement 3.1: Bubbles whitelisted/verified channels to the top
                return filteredResults.sortedWith(
                    compareByDescending<DevotionalVideo> { it.isVerified }
                        .thenByDescending { it.lengthSeconds > 0 } // Prefer items with valid duration
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed searching on $currentBaseUrl: ${e.message}. Rotating...", e)
                rotateInstance()
                attempts++
            }
        }

        // Return empty list if all attempts fail
        return emptyList()
    }

    /**
     * Requirement 4: Extract audio-only stream.
     * Tries to find the audio stream directly, or returns the proxy stream URL.
     */
    suspend fun getAudioStreamUrl(videoId: String): String {
        var attempts = 0
        val maxAttempts = 3 // try up to 3 rotating instances

        while (attempts < maxAttempts) {
            val currentBaseUrl = activeInstanceUrl
            try {
                val api = getApiClient(currentBaseUrl)
                val response = api.getVideoDetails(videoId)

                // Try adaptiveFormats (contains high quality audio-only webm/m4a streams)
                val audioFormats = response.adaptiveFormats ?: emptyList()
                val selectedFormat = audioFormats.filter { format ->
                    val type = format.type ?: ""
                    (type.startsWith("audio/") && (type.contains("mp4") || type.contains("m4a"))) ||
                    format.container == "m4a"
                }.maxByOrNull { format ->
                    // Prefer higher bitrates if available
                    format.bitrate?.toIntOrNull() ?: 0
                } ?: audioFormats.filter { format ->
                    val type = format.type ?: ""
                    type.startsWith("audio/") || format.container == "webm" || format.container == "m4a"
                }.maxByOrNull { format ->
                    format.bitrate?.toIntOrNull() ?: 0
                }

                var finalUrl = selectedFormat?.url
                if (!finalUrl.isNullOrEmpty()) {
                    if (finalUrl.startsWith("/")) {
                        finalUrl = currentBaseUrl.removeSuffix("/") + finalUrl
                    } else if (finalUrl.contains("googlevideo.com")) {
                        val uri = java.net.URI(finalUrl)
                        val pathAndQuery = uri.path + (if (uri.query != null) "?" + uri.query else "")
                        finalUrl = currentBaseUrl.removeSuffix("/") + pathAndQuery
                    }

                    if (!finalUrl.contains("local=true")) {
                        finalUrl = if (finalUrl.contains("?")) {
                            "$finalUrl&local=true"
                        } else {
                            "$finalUrl?local=true"
                        }
                    }
                    Log.d(TAG, "Successfully extracted proxied audio-only URL: $finalUrl")
                    return finalUrl
                }

                // Fallback to formatStreams (regular MP4/WebM with audio)
                val regularStream = response.formatStreams?.firstOrNull()?.url
                if (!regularStream.isNullOrEmpty()) {
                    var finalRegUrl = regularStream
                    if (finalRegUrl.startsWith("/")) {
                        finalRegUrl = currentBaseUrl.removeSuffix("/") + finalRegUrl
                    } else if (finalRegUrl.contains("googlevideo.com")) {
                        val uri = java.net.URI(finalRegUrl)
                        val pathAndQuery = uri.path + (if (uri.query != null) "?" + uri.query else "")
                        finalRegUrl = currentBaseUrl.removeSuffix("/") + pathAndQuery
                    }

                    if (!finalRegUrl.contains("local=true")) {
                        finalRegUrl = if (finalRegUrl.contains("?")) {
                            "$finalRegUrl&local=true"
                        } else {
                            "$finalRegUrl?local=true"
                        }
                    }
                    Log.d(TAG, "Using formatStream fallback URL: $finalRegUrl")
                    return finalRegUrl
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed retrieving details for $videoId from $currentBaseUrl: ${e.message}. Rotating...", e)
                rotateInstance()
                attempts++
            }
        }

        // Ultimate reliable fallback: Direct proxy audio URL which handles stream delivery on the fly
        val proxyUrl = "${activeInstanceUrl.removeSuffix("/")}/latest_version?id=$videoId&itag=140&local=true"
        Log.d(TAG, "Direct parsing failed or timed out. Delivering ultimate proxy stream fallback: $proxyUrl")
        return proxyUrl
    }
}
