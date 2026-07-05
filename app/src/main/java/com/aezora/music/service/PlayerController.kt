package com.aezora.music.service

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.SonicAudioProcessor
import com.aezora.music.domain.model.*
import com.aezora.music.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@UnstableApi
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository
) {
    val player: ExoPlayer

    // Sonic processor for pitch shifting
    private val sonicProcessor = SonicAudioProcessor()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null

    private var equalizer: Equalizer? = null

    init {
        player = ExoPlayer.Builder(context)
            .build()

        setupPlayerListener()
        startPositionTracking()
    }

    // ─── Player Listener ──────────────────────────────────────────────────────

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Update current track from queue
                val idx = player.currentMediaItemIndex
                val queue = _state.value.queue
                if (idx in queue.indices) {
                    _state.update { it.copy(currentTrack = queue[idx], queueIndex = idx) }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    _state.update { it.copy(isPlaying = false) }
                }
            }
        })
    }

    private fun startPositionTracking() {
        positionJob = scope.launch {
            while (true) {
                _state.update {
                    it.copy(
                        position = player.currentPosition,
                        duration = player.duration.takeIf { d -> d > 0 } ?: it.duration
                    )
                }
                delay(500)
            }
        }
    }

    // ─── Playback Control ─────────────────────────────────────────────────────

    fun play(tracks: List<Track>, startIndex: Int = 0) {
        scope.launch {
            val resolved = tracks.map { track ->
                if (track.localPath != null) {
                    track
                } else {
                    val url = repository.resolveStreamUrl(track) ?: track.streamUrl
                    track.copy(streamUrl = url)
                }
            }

            val items = resolved.map { track ->
                MediaItem.Builder()
                    .setUri(track.localPath ?: track.streamUrl)
                    .setMediaId(track.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(
                                if (track.artworkUrl.isNotBlank()) android.net.Uri.parse(track.artworkUrl) else null
                            )
                            .build()
                    )
                    .build()
            }

            player.setMediaItems(items, startIndex, 0)
            player.prepare()
            player.play()
            _state.update {
                it.copy(
                    queue = resolved,
                    queueIndex = startIndex,
                    currentTrack = resolved.getOrNull(startIndex)
                )
            }
        }
    }

    fun playTrack(track: Track) {
        val current = _state.value.queue
        val idx = current.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            player.seekTo(idx, 0)
            player.play()
        } else {
            play(listOf(track))
        }
    }

    fun updateStreamUrl(trackId: String, url: String) {
        val queue = _state.value.queue.toMutableList()
        val idx = queue.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            queue[idx] = queue[idx].copy(streamUrl = url)
            _state.update { it.copy(queue = queue) }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()

    fun setRepeatMode(mode: RepeatMode) {
        player.repeatMode = when (mode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        _state.update { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffled
        player.shuffleModeEnabled = newShuffle
        _state.update { it.copy(isShuffled = newShuffle) }
    }

    fun addToQueue(track: Track) {
        val queue = _state.value.queue.toMutableList()
        queue.add(track)
        _state.update { it.copy(queue = queue) }
        val uri = track.localPath ?: track.streamUrl
        player.addMediaItem(
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(track.id)
                .build()
        )
    }

    // ─── Speed / Pitch ────────────────────────────────────────────────────────

    fun setSpeedMode(mode: SpeedMode) {
        // Convert semitones to pitch multiplier: 2^(semitones/12)
        val pitchMultiplier = 2f.pow(mode.pitchSemitones / 12f)
        sonicProcessor.setPitch(pitchMultiplier)
        // Keep normal tempo (pitch shift only)
        sonicProcessor.setSpeed(1f)
        _state.update { it.copy(speedMode = mode) }
    }

    // ─── Equalizer ────────────────────────────────────────────────────────────

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, player.audioSessionId)
                equalizer?.enabled = true
            }
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            preset.bands.forEachIndexed { i, band ->
                if (i < numBands) {
                    val milliBel = (band.gainDb * 100).toInt()
                    eq.setBandLevel(i.toShort(), milliBel.toShort())
                }
            }
            _state.update { it.copy(equalizerPreset = preset) }
        } catch (e: Exception) {
            // Some devices don't support equalizer
        }
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }
}

// Extension for power
private fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()
