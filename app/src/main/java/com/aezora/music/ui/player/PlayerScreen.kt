package com.aezora.music.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.aezora.music.domain.model.*
import com.aezora.music.ui.*
import com.aezora.music.ui.theme.AezoraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onLike: (Track) -> Unit,
    onSetSpeedMode: (SpeedMode) -> Unit,
    onSetRepeat: (com.aezora.music.domain.model.RepeatMode) -> Unit,
    onToggleShuffle: () -> Unit,
    onApplyEq: (EqualizerPreset) -> Unit,
    onAddToQueue: () -> Unit
) {
    val track = playerState.currentTrack ?: return

    var showEqualizer by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AezoraColors.surface,
                        AezoraColors.background,
                        AezoraColors.background
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 80f) onDismiss()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = AezoraColors.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AezoraColors.container)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = null,
                            tint = AezoraColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Моя волна",
                            color = AezoraColors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
                IconButton(onClick = { /* more */ }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = AezoraColors.onSurface
                    )
                }
            }

            // ── Album Art ──────────────────────────────────────────────────────
            val artScale by animateFloatAsState(
                targetValue = if (playerState.isPlaying) 1f else 0.88f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "scale"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f)
                    .scale(artScale)
                    .clip(CircleShape)
                    .background(AezoraColors.container),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Rotating vinyl effect overlay
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AezoraColors.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AezoraColors.container)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Track Info + Like + Add ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = AezoraColors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = AezoraColors.onSurface.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onLike(track) }) {
                    Icon(
                        imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (track.isLiked) AezoraColors.primary else AezoraColors.onSurface.copy(0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        Icons.Filled.AddCircleOutline,
                        contentDescription = "Add to queue",
                        tint = AezoraColors.onSurface.copy(0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progress Bar ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Slider(
                    value = if (playerState.duration > 0)
                        (playerState.position.toFloat() / playerState.duration.toFloat())
                    else 0f,
                    onValueChange = { fraction ->
                        onSeek((fraction * playerState.duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AezoraColors.onSurface,
                        activeTrackColor = AezoraColors.primary,
                        inactiveTrackColor = AezoraColors.container
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(playerState.position), color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
                    Text(formatDuration(playerState.duration), color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Playback Controls ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AezoraColors.container.copy(alpha = 0.6f))
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Repeat
                    IconButton(onClick = {
                        val next = when (playerState.repeatMode) {
                            RepeatMode.NONE -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.NONE
                        }
                        onSetRepeat(next)
                    }) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.ONE -> Icons.Filled.RepeatOne
                                else -> Icons.Filled.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.NONE)
                                AezoraColors.primary else AezoraColors.onSurface.copy(0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Previous
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = AezoraColors.onSurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Play/Pause
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    // Next
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = AezoraColors.onSurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Shuffle
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffled)
                                AezoraColors.primary else AezoraColors.onSurface.copy(0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bottom controls ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AezoraColors.container.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equalizer
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Equalizer",
                                tint = AezoraColors.onSurface.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text("EQ", color = AezoraColors.onSurface.copy(0.5f), fontSize = 10.sp)
                    }
                    // Speed Mode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showSpeedSheet = true }) {
                            Icon(
                                Icons.Filled.Speed,
                                contentDescription = "Speed",
                                tint = if (playerState.speedMode != SpeedMode.NORMAL)
                                    AezoraColors.primary else AezoraColors.onSurface.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            playerState.speedMode.label,
                            color = if (playerState.speedMode != SpeedMode.NORMAL)
                                AezoraColors.primary else AezoraColors.onSurface.copy(0.5f),
                            fontSize = 10.sp
                        )
                    }
                    // Queue
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgedBox(
                            badge = {
                                if (playerState.queue.isNotEmpty()) {
                                    Badge { Text("${playerState.queue.size}") }
                                }
                            }
                        ) {
                            IconButton(onClick = { showQueueSheet = true }) {
                                Icon(
                                    Icons.Filled.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = AezoraColors.onSurface.copy(0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text("Очередь", color = AezoraColors.onSurface.copy(0.5f), fontSize = 10.sp)
                    }
                }
            }
        }

        // ── Speed Sheet ───────────────────────────────────────────────────────
        if (showSpeedSheet) {
            SpeedModeSheet(
                current = playerState.speedMode,
                onSelect = { mode ->
                    onSetSpeedMode(mode)
                    showSpeedSheet = false
                },
                onDismiss = { showSpeedSheet = false }
            )
        }

        // ── Equalizer Sheet ───────────────────────────────────────────────────
        if (showEqualizer) {
            EqualizerSheet(
                currentPreset = playerState.equalizerPreset,
                onSelect = { preset ->
                    onApplyEq(preset)
                    showEqualizer = false
                },
                onDismiss = { showEqualizer = false }
            )
        }

        // ── Queue Sheet ───────────────────────────────────────────────────────
        if (showQueueSheet) {
            QueueSheet(
                queue = playerState.queue,
                currentIndex = playerState.queueIndex,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

// ─── Speed Mode Sheet ─────────────────────────────────────────────────────────

@Composable
fun SpeedModeSheet(
    current: SpeedMode,
    onSelect: (SpeedMode) -> Unit,
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
            Text(
                "Скорость / Pitch",
                color = AezoraColors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            SpeedMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (current == mode) AezoraColors.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, desc) = when (mode) {
                        SpeedMode.NORMAL -> Icons.Filled.GraphicEq to "Обычный"
                        SpeedMode.SPEED_UP -> Icons.Filled.FastForward to "+2 тона выше"
                        SpeedMode.ULTRA_SPEED_UP -> Icons.Filled.FastForward to "+4 тона выше"
                        SpeedMode.SLOWED -> Icons.Filled.FastRewind to "-2 тона ниже"
                        SpeedMode.ULTRA_SLOWED -> Icons.Filled.FastRewind to "-4 тона ниже"
                    }
                    Icon(icon, contentDescription = null, tint = AezoraColors.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(mode.label, color = AezoraColors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(desc, color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (current == mode) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AezoraColors.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Equalizer Sheet ──────────────────────────────────────────────────────────

@Composable
fun EqualizerSheet(
    currentPreset: EqualizerPreset,
    onSelect: (EqualizerPreset) -> Unit,
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
            Text("Эквалайзер", color = AezoraColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            EqualizerPreset.ALL.forEach { preset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (currentPreset.name == preset.name) AezoraColors.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(preset) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.GraphicEq, null, tint = AezoraColors.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(preset.name, color = AezoraColors.onSurface, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    if (currentPreset.name == preset.name) {
                        Icon(Icons.Filled.CheckCircle, null, tint = AezoraColors.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Queue Sheet ──────────────────────────────────────────────────────────────

@Composable
fun QueueSheet(
    queue: List<Track>,
    currentIndex: Int,
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
                .fillMaxHeight(0.6f)
                .clickable(enabled = false) {}
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AezoraColors.surface)
                .padding(vertical = 16.dp)
        ) {
            Text(
                "Очередь",
                color = AezoraColors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                queue.forEachIndexed { i, track ->
                    TrackRow(
                        track = track,
                        isPlaying = i == currentIndex
                    )
                }
            }
        }
    }
}
