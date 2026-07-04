package com.aezora.music.ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aezora.music.domain.model.*
import com.aezora.music.service.MusicService
import com.aezora.music.ui.home.*
import com.aezora.music.ui.library.LibraryScreen
import com.aezora.music.ui.player.PlayerScreen
import com.aezora.music.ui.settings.SettingsScreen
import com.aezora.music.ui.theme.AezoraColors
import com.aezora.music.ui.theme.AezoraTheme
import dagger.hilt.android.AndroidEntryPoint

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Главная", Icons.Filled.Home),
    SEARCH("Поиск", Icons.Filled.Search),
    LIBRARY("Библиотека", Icons.Filled.LibraryMusic),
    SETTINGS("Настройки", Icons.Filled.Settings)
}

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Start music service
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            AezoraTheme(appTheme = appTheme) {
                AezoraApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AezoraApp(viewModel: MainViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val downloadQuality by viewModel.downloadQuality.collectAsState()
    val streamQuality by viewModel.streamQuality.collectAsState()
    val persistSpeed by viewModel.persistSpeed.collectAsState()

    var currentDest by remember { mutableStateOf(NavDestination.HOME) }
    var showPlayer by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf<Track?>(null) }

    val hasYandex by remember(homeState) { derivedStateOf { homeState.yandexPlaylists.isNotEmpty() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AezoraColors.background)
    ) {
        // ── Main Content ──────────────────────────────────────────────────────
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (currentDest) {
                    NavDestination.HOME -> HomeScreen(
                        trendingTracks = homeState.trendingTracks,
                        yandexPlaylists = homeState.yandexPlaylists,
                        isLoading = homeState.isLoading,
                        hasYandex = hasYandex,
                        onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                        onPlaylistClick = { /* open playlist */ },
                        onRefresh = { viewModel.loadHome() }
                    )
                    NavDestination.SEARCH -> SearchScreen(
                        query = searchState.query,
                        results = searchState.results.tracks,
                        isLoading = searchState.isLoading,
                        onQueryChange = { viewModel.search(it) },
                        onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                        onLike = { viewModel.toggleLike(it) },
                        onDownload = { viewModel.downloadTrack(it) },
                        onAddToPlaylist = { showAddToPlaylistSheet = it }
                    )
                    NavDestination.LIBRARY -> LibraryScreen(
                        likedTracks = libraryState.likedTracks,
                        userPlaylists = libraryState.userPlaylists,
                        downloadedTracks = libraryState.downloadedTracks,
                        onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                        onLike = { viewModel.toggleLike(it) },
                        onDownload = { viewModel.downloadTrack(it) },
                        onCreatePlaylist = { viewModel.createPlaylist(it) },
                        onPlaylistClick = { /* open playlist detail */ },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onDownloadPlaylist = { viewModel.downloadPlaylist(it) }
                    )
                    NavDestination.SETTINGS -> SettingsScreen(
                        currentTheme = appTheme,
                        downloadQuality = downloadQuality,
                        streamQuality = streamQuality,
                        persistSpeed = persistSpeed,
                        onSetTheme = { viewModel.setAppTheme(it) },
                        onSetDownloadQuality = { viewModel.setDownloadQuality(it) },
                        onSetStreamQuality = { viewModel.setStreamQuality(it) },
                        onSetYandexToken = { viewModel.setYandexToken(it) },
                        onSetPersistSpeed = { viewModel.setPersistSpeed(it) }
                    )
                }
            }

            // ── Mini Player ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = playerState.currentTrack != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                playerState.currentTrack?.let { track ->
                    MiniPlayerBar(
                        track = track,
                        isPlaying = playerState.isPlaying,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onLike = { viewModel.toggleLike(track) },
                        onExpand = { showPlayer = true }
                    )
                }
            }

            // ── Bottom Navigation ─────────────────────────────────────────────
            BottomNavBar(
                currentDest = currentDest,
                onDestChange = { currentDest = it }
            )
        }

        // ── Full Player Overlay ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            PlayerScreen(
                playerState = playerState,
                onDismiss = { showPlayer = false },
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeek = { viewModel.seekTo(it) },
                onLike = { viewModel.toggleLike(it) },
                onSetSpeedMode = { viewModel.setSpeedMode(it) },
                onSetRepeat = { viewModel.setRepeatMode(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onApplyEq = { viewModel.applyEqPreset(it) },
                onAddToQueue = { /* show track picker */ }
            )
        }

        // ── Add to Playlist Sheet ─────────────────────────────────────────────
        showAddToPlaylistSheet?.let { track ->
            AddToPlaylistSheet(
                playlists = libraryState.userPlaylists,
                onSelect = { playlist ->
                    viewModel.addToPlaylist(playlist.id, track)
                    showAddToPlaylistSheet = null
                },
                onDismiss = { showAddToPlaylistSheet = null }
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    currentDest: NavDestination,
    onDestChange: (NavDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AezoraColors.surface)
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavDestination.values().forEach { dest ->
            val selected = currentDest == dest
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDestChange(dest) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    tint = if (selected) AezoraColors.primary else AezoraColors.onSurface.copy(0.45f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dest.label,
                    color = if (selected) AezoraColors.primary else AezoraColors.onSurface.copy(0.45f),
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onSelect: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AezoraColors.surface)
                .padding(24.dp)
        ) {
            Text("Добавить в плейлист", color = AezoraColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            if (playlists.isEmpty()) {
                Text("Нет плейлистов. Создайте в Библиотеке.", color = AezoraColors.onSurface.copy(0.5f), fontSize = 14.sp)
            } else {
                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(pl) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LibraryMusic, null, tint = AezoraColors.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(pl.name, color = AezoraColors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${pl.tracks.size} треков", color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
