package com.aezora.music.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.aezora.music.domain.model.*
import com.aezora.music.ui.*
import com.aezora.music.ui.theme.AezoraColors

@Composable
fun HomeScreen(
    trendingTracks: List<Track>,
    yandexPlaylists: List<Playlist>,
    isLoading: Boolean,
    hasYandex: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AezoraColors.background)
    ) {
        if (isLoading && trendingTracks.isEmpty()) {
            CircularProgressIndicator(
                color = AezoraColors.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                item {
                    HomeHeader(
                        onRefresh = onRefresh,
                        onPlayAll = {
                            if (trendingTracks.isNotEmpty())
                                onTrackClick(trendingTracks.first(), trendingTracks)
                        }
                    )
                }

                // ── My Wave Button ────────────────────────────────────────────
                item {
                    MyWaveButton(
                        onClick = {
                            if (trendingTracks.isNotEmpty())
                                onTrackClick(trendingTracks.random(), trendingTracks)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Yandex Playlists (if token set) ───────────────────────────
                if (hasYandex && yandexPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Мои плейлисты Я.Музыки")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(yandexPlaylists) { pl ->
                                PlaylistCard(playlist = pl, onClick = { onPlaylistClick(pl) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Popular / Trending ────────────────────────────────────────
                item {
                    SectionHeader(title = "Популярное")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "  Треки",
                        color = AezoraColors.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingTracks.take(10)) { track ->
                            TrackCard(track = track, onClick = { onTrackClick(track, trendingTracks) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── All trending as rows ──────────────────────────────────────
                item {
                    Text(
                        "  Плейлисты",
                        color = AezoraColors.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(trendingTracks.drop(10)) { track ->
                    TrackRow(
                        track = track,
                        onPlay = { onTrackClick(track, trendingTracks) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onRefresh: () -> Unit,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AezoraColors.primary.copy(alpha = 0.25f),
                        AezoraColors.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Aezora",
                color = AezoraColors.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Музыка без границ",
                color = AezoraColors.onSurface.copy(alpha = 0.55f),
                fontSize = 14.sp
            )
        }
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, "Refresh", tint = AezoraColors.onSurface.copy(0.7f))
        }
    }
}

@Composable
private fun MyWaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        AezoraColors.primary.copy(alpha = 0.7f),
                        AezoraColors.secondary.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Моя волна", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Персональные рекомендации", color = Color.White.copy(0.7f), fontSize = 13.sp)
            }
        }
        // Wave decoration
        WaveDecoration(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 48.dp)
        )
    }
}

@Composable
private fun WaveDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(20.dp)) {
        val path = androidx.compose.ui.graphics.Path()
        val w = size.width
        val h = size.height
        path.moveTo(0f, h / 2)
        for (i in 0..10) {
            val x = w * i / 10f
            val y = if (i % 2 == 0) h * 0.2f else h * 0.8f
            path.quadraticBezierTo(x, y, x + w / 20, h / 2)
        }
        drawPath(path, Color.White.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
    }
}
