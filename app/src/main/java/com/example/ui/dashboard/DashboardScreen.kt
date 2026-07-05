package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.manager.PlaybackState
import com.example.data.model.DevotionalVideo
import com.example.ui.viewmodel.BhajanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BhajanViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playerErrorMessage by viewModel.playerErrorMessage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Quick search tags
    val suggestionTags = listOf("Maha Mantra", "Srila Prabhupada", "Aindra Prabhu", "Mayapur Kirtan", "Damodarastakam")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header Banner Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_devotional_banner),
                        contentDescription = "Devotional Banner Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Saffron-to-dark scrim overlay for typography depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Prabhupada Vani",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Distraction-Free ISKCON Bhajan Player",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentTrack != null) 145.dp else 0.dp) // Leave exact space for bottom player bar
            ) {
                // 1. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search bhajans, kirtans, or mantras...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search query"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Suggestion Tags Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestionTags) { tag ->
                        SuggestionChip(
                            onClick = {
                                viewModel.onSearchQueryChanged(tag)
                                viewModel.performSearch(tag)
                                keyboardController?.hide()
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Search Button (if typing but not searched yet)
                Button(
                    onClick = {
                        viewModel.performSearch(searchQuery)
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search Devotional Library")
                }

                // 3. Custom M3 Pill Tabs (Explore vs Favorites)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val isExplore = activeTab == "explore"
                    val isFavorites = activeTab == "favorites"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isExplore) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { viewModel.onTabChanged("explore") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Explore",
                                tint = if (isExplore) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Explore",
                                fontWeight = FontWeight.Bold,
                                color = if (isExplore) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFavorites) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { viewModel.onTabChanged("favorites") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = if (isFavorites) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "My Favorites",
                                fontWeight = FontWeight.Bold,
                                color = if (isFavorites) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 4. Loading indicator / Error Banner / Tracks List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Scraping public servers...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning icon",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Display either Explore Tracks or Favorites List
                    val activeTrackList = if (activeTab == "explore") {
                        tracks
                    } else {
                        favorites.map { fav ->
                            DevotionalVideo(
                                videoId = fav.videoId,
                                title = fav.title,
                                author = fav.author,
                                authorId = fav.authorId,
                                thumbnailUrl = fav.thumbnailUrl,
                                lengthSeconds = fav.lengthSeconds,
                                durationText = fav.durationText,
                                isVerified = true
                            )
                        }
                    }

                    if (activeTrackList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeTab == "explore") Icons.Outlined.Search else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Empty list icon",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (activeTab == "explore") "Search for 'Madhava', 'Aindra' or 'Maha Mantra'" else "No bookmarks saved yet.",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = if (activeTab == "explore") "We'll scrub public Invidious servers and find verified devotional results." else "Click the heart icon on any bhajan to save it here for quick access.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activeTrackList, key = { it.videoId }) { bhajan ->
                                val isFavoritedFlow = viewModel.isFavoriteFlow(bhajan.videoId)
                                val isFavorited by isFavoritedFlow.collectAsStateWithLifecycle(initialValue = false)

                                DevotionalTrackCard(
                                    bhajan = bhajan,
                                    isFavorited = isFavorited,
                                    onPlayClick = { viewModel.selectAndPlayTrack(bhajan) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(bhajan) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Ambient Background Audio Player Drawer (Overlapping Player at Bottom)
            AnimatedVisibility(
                visible = currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                currentTrack?.let { track ->
                    BottomAudioPlayer(
                        track = track,
                        playbackState = playbackState,
                        currentPosition = currentPosition,
                        duration = duration,
                        playerErrorMessage = playerErrorMessage,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.playNextTrack() },
                        onPrevClick = { viewModel.playPreviousTrack() },
                        onSeek = { viewModel.seekTo(it) }
                    )
                }
            }
        }
    }
}

/**
 * Single Bhajans/Kirtan track list card
 */
@Composable
fun DevotionalTrackCard(
    bhajan: DevotionalVideo,
    isFavorited: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail using Coil
            AsyncImage(
                model = bhajan.thumbnailUrl,
                contentDescription = "${bhajan.title} thumbnail",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_background),
                error = painterResource(id = R.drawable.ic_launcher_background),
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = bhajan.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bhajan.author,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•  ${bhajan.durationText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Verified Badges for genuine whitelisted channels
                if (bhajan.isVerified) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified ISKCON Source",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Favorite/Heart Toggle Icon
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite Toggle Button",
                    tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * High fidelity responsive bottom audio player
 */
@Composable
fun BottomAudioPlayer(
    track: DevotionalVideo,
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    playerErrorMessage: String?,
    onPlayPauseToggle: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Main Track details & play buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round album artwork
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = "Playing track thumbnail",
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Text labels
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = track.author,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Media Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onPrevClick) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play/Pause button with distinct dynamic state
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .clickable { onPlayPauseToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackState == PlaybackState.LOADING) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause Button",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNextClick) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track Loading Fallback Error Banner
            if (!playerErrorMessage.isNullOrEmpty()) {
                Text(
                    text = playerErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            // High precision seek slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimeMs(currentPosition),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )

                Text(
                    text = if (duration > 0) formatTimeMs(duration) else track.durationText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Format timestamp (ms) -> MM:SS
 */
private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
