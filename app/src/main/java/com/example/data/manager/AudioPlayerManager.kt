package com.example.data.manager

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.model.DevotionalVideo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    ERROR
}

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var preparationTimeoutJob: Job? = null

    private val _currentTrack = MutableStateFlow<DevotionalVideo?>(null)
    val currentTrack: StateFlow<DevotionalVideo?> = _currentTrack

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var onTrackComplete: (() -> Unit)? = null
    var onErrorOccurred: ((Int, Int) -> Unit)? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MediaPlayer Prepared. Starting playback.")
                    preparationTimeoutJob?.cancel()
                    _playbackState.value = PlaybackState.PLAYING
                    _duration.value = mp.duration.toLong()
                    mp.start()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    Log.d(TAG, "MediaPlayer Completed track.")
                    preparationTimeoutJob?.cancel()
                    _playbackState.value = PlaybackState.IDLE
                    _currentPosition.value = 0L
                    stopProgressTracker()
                    onTrackComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer Error occurred. What: $what, Extra: $extra")
                    preparationTimeoutJob?.cancel()
                    _playbackState.value = PlaybackState.ERROR
                    _errorMessage.value = "Streaming error ($what). Trying fallback..."
                    stopProgressTracker()
                    onErrorOccurred?.invoke(what, extra)
                    true
                }
            }
        }
    }

    fun play(context: android.content.Context, track: DevotionalVideo, streamUrl: String) {
        scope.launch {
            try {
                Log.d(TAG, "Playing track: ${track.title} with URL: $streamUrl")
                _currentTrack.value = track
                _playbackState.value = PlaybackState.LOADING
                _errorMessage.value = null
                _currentPosition.value = 0L
                _duration.value = if (track.lengthSeconds > 0) track.lengthSeconds * 1000L else 0L

                stopProgressTracker()
                preparationTimeoutJob?.cancel()

                // Launch 8-second safety timeout for stream preparation
                preparationTimeoutJob = scope.launch {
                    delay(8000)
                    if (_playbackState.value == PlaybackState.LOADING) {
                        Log.w(TAG, "Media preparation timed out (8s). Forcing automatic server rotation fallback...")
                        withContext(Dispatchers.IO) {
                            try {
                                mediaPlayer?.reset()
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error resetting player on timeout", ex)
                            }
                        }
                        _playbackState.value = PlaybackState.ERROR
                        _errorMessage.value = "Server timed out. Retrying with alternate node..."
                        onErrorOccurred?.invoke(-1, -1)
                    }
                }

                // Reset and prepare MediaPlayer on IO thread
                withContext(Dispatchers.IO) {
                    mediaPlayer?.reset()
                    
                    try {
                        val headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        )
                        mediaPlayer?.setDataSource(
                            context.applicationContext,
                            android.net.Uri.parse(streamUrl),
                            headers
                        )
                    } catch (ex: Exception) {
                        Log.w(TAG, "Header-based setDataSource failed. Retrying with basic datasource...", ex)
                        mediaPlayer?.reset()
                        mediaPlayer?.setDataSource(streamUrl)
                    }
                    mediaPlayer?.prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up playback: ${e.message}", e)
                _playbackState.value = PlaybackState.ERROR
                _errorMessage.value = "Unable to load stream. Please try again."
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = PlaybackState.PAUSED
                stopProgressTracker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (_playbackState.value == PlaybackState.PAUSED) {
                it.start()
                _playbackState.value = PlaybackState.PLAYING
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun togglePlayPause() {
        when (_playbackState.value) {
            PlaybackState.PLAYING -> pause()
            PlaybackState.PAUSED -> resume()
            else -> { /* No op */ }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
