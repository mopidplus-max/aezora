package com.aezora.music.ui.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.aezora.music.domain.model.*
import com.aezora.music.ui.*
import com.aezora.music.ui.theme.AezoraColors

enum class LibraryTab { LIKED, PLAYLISTS, DOWNLOADED }

@Composable
fun LibraryScreen(
    likedTracks: List<Track>,
    userPlaylists: List<Playlist>,
    downloadedTracks: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onLike: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDownloadPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.LIKED) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AezoraColors.background)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Библиотека",
                color = AezoraColors.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                modifier = Modifier.weight(1f)
            )
            if (selectedTab == LibraryTab.PLAYLISTS) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, null, tint = AezoraColors.primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AezoraColors.container),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LibraryTab.values().forEach { tab ->
                val (label, icon) = when (tab) {
                    LibraryTab.LIKED -> "Любимые" to Icons.Filled.Favorite
                    LibraryTab.PLAYLISTS -> "Плейлисты" to Icons.Filled.LibraryMusic
                    LibraryTab.DOWNLOADED -> "Скачанные" to Icons.Filled.Download
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedTab == tab) AezoraColors.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            icon, null,
                            tint = if (selectedTab == tab) AezoraColors.primary else AezoraColors.onSurface.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            label,
                            color = if (selectedTab == tab) AezoraColors.primary else AezoraColors.onSurface.copy(0.5f),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content
        when (selectedTab) {
            LibraryTab.LIKED -> TrackList(
                tracks = likedTracks,
                emptyMessage = "Нет любимых треков",
                emptyIcon = Icons.Filled.FavoriteBorder,
                onTrackClick = onTrackClick,
                onLike = onLike,
                onDownload = onDownload
            )
            LibraryTab.PLAYLISTS -> PlaylistList(
                playlists = userPlaylists,
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylist = onDeletePlaylist,
                onDownloadPlaylist = onDownloadPlaylist
            )
            LibraryTab.DOWNLOADED -> TrackList(
                tracks = downloadedTracks,
                emptyMessage = "Нет скачанных треков",
                emptyIcon = Icons.Filled.DownloadDone,
                onTrackClick = onTrackClick,
                onLike = onLike,
                onDownload = {}
            )
        }
    }

    // Create playlist dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    emptyMessage: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onTrackClick: (Track, List<Track>) -> Unit,
    onLike: (Track) -> Unit,
    onDownload: (Track) -> Unit
) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(emptyIcon, null, tint = AezoraColors.onSurface.copy(0.25f), modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(12.dp))
                Text(emptyMessage, color = AezoraColors.onSurface.copy(0.4f), fontSize = 16.sp)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onPlay = { onTrackClick(track, tracks) },
                    onLike = { onLike(track) },
                    onMore = { onDownload(track) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDownloadPlaylist: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.LibraryMusic, null, tint = AezoraColors.onSurface.copy(0.25f), modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(12.dp))
                Text("Нет плейлистов", color = AezoraColors.onSurface.copy(0.4f), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Нажмите + чтобы создать", color = AezoraColors.onSurface.copy(0.3f), fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = { onDeletePlaylist(playlist.id) },
                    onDownload = { onDownloadPlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AezoraColors.container),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.artworkUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = AezoraColors.primary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, color = AezoraColors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${playlist.tracks.size} треков", color = AezoraColors.onSurface.copy(0.5f), fontSize = 13.sp)
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Filled.Download, null, tint = AezoraColors.onSurface.copy(0.5f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, null, tint = AezoraColors.onSurface.copy(0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AezoraColors.surface,
        title = { Text("Новый плейлист", color = AezoraColors.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название", color = AezoraColors.onSurface.copy(0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AezoraColors.onSurface,
                    unfocusedTextColor = AezoraColors.onSurface,
                    focusedBorderColor = AezoraColors.primary,
                    unfocusedBorderColor = AezoraColors.container,
                    cursorColor = AezoraColors.primary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Создать", color = AezoraColors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AezoraColors.onSurface.copy(0.5f))
            }
        }
    )
}
