package com.aezora.music.ui.home

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
import com.aezora.music.domain.model.Track
import com.aezora.music.ui.*
import com.aezora.music.ui.theme.AezoraColors

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onLike: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMore by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AezoraColors.background)
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Поиск",
            color = AezoraColors.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))

        // Search Field
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text("Трек, исполнитель...", color = AezoraColors.onSurface.copy(0.4f))
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, tint = AezoraColors.onSurface.copy(0.6f))
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, null, tint = AezoraColors.onSurface.copy(0.6f))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AezoraColors.container,
                unfocusedContainerColor = AezoraColors.container,
                focusedTextColor = AezoraColors.onSurface,
                unfocusedTextColor = AezoraColors.onSurface,
                cursorColor = AezoraColors.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AezoraColors.primary)
            }
            query.isBlank() -> SearchHints()
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, null, tint = AezoraColors.onSurface.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Ничего не найдено", color = AezoraColors.onSurface.copy(0.5f), fontSize = 16.sp)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Text(
                        "Результаты: ${results.size}",
                        color = AezoraColors.onSurface.copy(0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(results, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onPlay = { onTrackClick(track, results) },
                        onLike = { onLike(track) },
                        onMore = { showMore = track }
                    )
                }
            }
        }
    }

    // More options sheet
    showMore?.let { track ->
        TrackOptionsSheet(
            track = track,
            onDismiss = { showMore = null },
            onDownload = { onDownload(track); showMore = null },
            onAddToPlaylist = { onAddToPlaylist(track); showMore = null },
            onLike = { onLike(track); showMore = null }
        )
    }
}

@Composable
private fun SearchHints() {
    val hints = listOf("SoundCloud", "Яндекс Музыка", "Популярные треки", "Плейлисты")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Поищите что-нибудь", color = AezoraColors.onSurface.copy(0.6f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        hints.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { hint ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AezoraColors.container)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(hint, color = AezoraColors.onSurface.copy(0.7f), fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun TrackOptionsSheet(
    track: com.aezora.music.domain.model.Track,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onLike: () -> Unit
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
            Text(track.title, color = AezoraColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(track.artist, color = AezoraColors.onSurface.copy(0.5f), fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            listOf(
                Triple(Icons.Filled.Download, "Скачать", onDownload),
                Triple(Icons.Filled.PlaylistAdd, "Добавить в плейлист", onAddToPlaylist),
                Triple(
                    if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (track.isLiked) "Убрать из любимых" else "В любимые",
                    onLike
                )
            ).forEach { (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { action() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = AezoraColors.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(label, color = AezoraColors.onSurface, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
