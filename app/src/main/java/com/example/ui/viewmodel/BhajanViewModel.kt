package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteBhajan
import com.example.data.db.FavoriteRepository
import com.example.data.manager.AudioPlayerManager
import com.example.data.manager.DevotionalStreamManager
import com.example.data.manager.PlaybackState
import com.example.data.model.DevotionalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BhajanViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BhajanViewModel"

    private val database = AppDatabase.getDatabase(application)
    private val favoriteRepository = FavoriteRepository(database.favoriteDao())
    private val streamManager = DevotionalStreamManager()

    // UI state flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _tracks = MutableStateFlow<List<DevotionalVideo>>(emptyList())
    val tracks: StateFlow<List<DevotionalVideo>> = _tracks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Screen navigation tabs: "explore" or "favorites"
    private val _activeTab = MutableStateFlow("explore")
    val activeTab: StateFlow<String> = _activeTab

    // Observe Room Favorites reactively
    val favorites: StateFlow<List<FavoriteBhajan>> = favoriteRepository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current playlist queue (either the search results or favorites list)
    private val _currentPlaylistQueue = MutableStateFlow<List<DevotionalVideo>>(emptyList())
    val currentPlaylistQueue: StateFlow<List<DevotionalVideo>> = _currentPlaylistQueue

    // Expose active audio player state
    val currentTrack = AudioPlayerManager.currentTrack
    val playbackState = AudioPlayerManager.playbackState
    val currentPosition = AudioPlayerManager.currentPosition
    val duration = AudioPlayerManager.duration
    val playerErrorMessage = AudioPlayerManager.errorMessage

    init {
        // Set up automatic playlist progression when a track finishes playing
        AudioPlayerManager.onTrackComplete = {
            playNextTrack()
        }

        // Set up self-healing automatic fallback rotation when playing errors occur
        AudioPlayerManager.onErrorOccurred = { _, _ ->
            handlePlaybackFailure()
        }

        // Trigger a default bhajan search on initial launch to keep screen vibrant
        performSearch("Maha Mantra")
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTabChanged(tabName: String) {
        _activeTab.value = tabName
        // Sync queue appropriately
        if (tabName == "favorites") {
            syncFavoritesToQueue()
        } else {
            _currentPlaylistQueue.value = _tracks.value
        }
    }

    private fun syncFavoritesToQueue() {
        _currentPlaylistQueue.value = favorites.value.map { fav ->
            DevotionalVideo(
                videoId = fav.videoId,
                title = fav.title,
                author = fav.author,
                authorId = fav.authorId,
                thumbnailUrl = fav.thumbnailUrl,
                lengthSeconds = fav.lengthSeconds,
                durationText = fav.durationText,
                isVerified = true // items saved in favorites are automatically trusted/verified
            )
        }
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _activeTab.value = "explore" // Switch back to explore to see results

            val results = streamManager.searchDevotionalTracks(query)
            if (results.isEmpty()) {
                _errorMessage.value = "No devotional results found. Check network connection or try another query."
            } else {
                _tracks.value = results
                _currentPlaylistQueue.value = results
            }
            _isLoading.value = false
        }
    }

    private var playAttemptCount = 0
    private var currentTrackToPlay: DevotionalVideo? = null

    /**
     * Resolves the stream URL and commands the player to begin stream.
     */
    fun selectAndPlayTrack(track: DevotionalVideo) {
        playAttemptCount = 0
        currentTrackToPlay = track
        // If switching playlist sources, make sure queue is set
        if (_activeTab.value == "favorites") {
            syncFavoritesToQueue()
        } else {
            _currentPlaylistQueue.value = _tracks.value
        }
        executePlayWithStream(track)
    }

    private fun executePlayWithStream(track: DevotionalVideo) {
        viewModelScope.launch {
            try {
                // Call manager to extract the optimized stream URL (webm/m4a)
                val audioUrl = streamManager.getAudioStreamUrl(track.videoId)
                Log.d(TAG, "Fetched audio stream URL: $audioUrl for track: ${track.title}")
                AudioPlayerManager.play(getApplication(), track, audioUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to play track ${track.title}: ${e.message}", e)
                handlePlaybackFailure()
            }
        }
    }

    private fun handlePlaybackFailure() {
        val track = currentTrackToPlay ?: return
        if (playAttemptCount < 4) {
            playAttemptCount++
            Log.w(TAG, "Playback failed. Rotating server and retrying (attempt $playAttemptCount)...")
            streamManager.rotateInstance()
            executePlayWithStream(track)
        } else {
            Log.e(TAG, "All play attempts failed for track ${track.title}.")
            _errorMessage.value = "Unable to play track. Try another bhajan or query."
        }
    }

    fun togglePlayPause() {
        AudioPlayerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        AudioPlayerManager.seekTo(positionMs)
    }

    fun toggleFavorite(track: DevotionalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteRepository.isFavoriteDirect(track.videoId)
            if (isFav) {
                favoriteRepository.deleteById(track.videoId)
            } else {
                favoriteRepository.insert(
                    FavoriteBhajan(
                        videoId = track.videoId,
                        title = track.title,
                        author = track.author,
                        authorId = track.authorId,
                        thumbnailUrl = track.thumbnailUrl,
                        lengthSeconds = track.lengthSeconds,
                        durationText = track.durationText
                    )
                )
            }
            // If currently on favorites tab, dynamically update the playing queue so next/prev works flawlessly
            if (_activeTab.value == "favorites") {
                syncFavoritesToQueue()
            }
        }
    }

    fun isFavoriteFlow(videoId: String): Flow<Boolean> {
        return favoriteRepository.isFavorite(videoId)
    }

    fun playNextTrack() {
        val queue = _currentPlaylistQueue.value
        val activeTrack = currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.videoId == activeTrack.videoId }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            val nextTrack = queue[currentIndex + 1]
            selectAndPlayTrack(nextTrack)
        } else if (queue.isNotEmpty()) {
            // Loop back to first track in playlist
            selectAndPlayTrack(queue[0])
        }
    }

    fun playPreviousTrack() {
        val queue = _currentPlaylistQueue.value
        val activeTrack = currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.videoId == activeTrack.videoId }
        if (currentIndex > 0) {
            val prevTrack = queue[currentIndex - 1]
            selectAndPlayTrack(prevTrack)
        } else if (queue.isNotEmpty()) {
            // Go to the last track in playlist
            selectAndPlayTrack(queue[queue.size - 1])
        }
    }

    override fun onCleared() {
        super.onCleared()
        AudioPlayerManager.release()
    }
}
