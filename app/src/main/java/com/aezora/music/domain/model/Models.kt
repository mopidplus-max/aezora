package com.aezora.music.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ─── Track ───────────────────────────────────────────────────────────────────

@Parcelize
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val duration: Long = 0L,           // ms
    val source: TrackSource,
    val streamUrl: String = "",        // resolved at play-time
    val localPath: String? = null,     // cached/downloaded file
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val hasDrm: Boolean = false,       // SoundCloud DRM flag
    val genre: String = "",
    val playCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class TrackSource { SOUNDCLOUD, YANDEX, LOCAL }

// ─── Playlist ─────────────────────────────────────────────────────────────────

@Parcelize
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val artworkUrl: String = "",
    val tracks: List<Track> = emptyList(),
    val isUserCreated: Boolean = true,
    val source: PlaylistSource = PlaylistSource.LOCAL,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class PlaylistSource { LOCAL, YANDEX, SOUNDCLOUD }

// ─── Audio Effects ────────────────────────────────────────────────────────────

enum class SpeedMode(val label: String, val pitchSemitones: Float, val speed: Float) {
    NORMAL("Normal", 0f, 1f),
    SPEED_UP("Speed UP", 2f, 1.0f),
    ULTRA_SPEED_UP("Ultra Speed UP", 4f, 1.0f),
    SLOWED("Slowed", -2f, 1.0f),
    ULTRA_SLOWED("Ultra Slowed", -4f, 1.0f)
}

data class EqualizerBand(
    val index: Int,
    val centerFrequencyHz: Int,
    val gainDb: Float = 0f   // -15 to +15
)

data class EqualizerPreset(
    val name: String,
    val bands: List<EqualizerBand>
) {
    companion object {
        val FLAT = EqualizerPreset("Flat", listOf(
            EqualizerBand(0, 60), EqualizerBand(1, 230),
            EqualizerBand(2, 910), EqualizerBand(3, 3600),
            EqualizerBand(4, 14000)
        ))
        val BASS_BOOST = EqualizerPreset("Bass Boost", listOf(
            EqualizerBand(0, 60, 6f), EqualizerBand(1, 230, 4f),
            EqualizerBand(2, 910, 0f), EqualizerBand(3, 3600, 0f),
            EqualizerBand(4, 14000, 0f)
        ))
        val TREBLE_BOOST = EqualizerPreset("Treble Boost", listOf(
            EqualizerBand(0, 60, 0f), EqualizerBand(1, 230, 0f),
            EqualizerBand(2, 910, 2f), EqualizerBand(3, 3600, 4f),
            EqualizerBand(4, 14000, 6f)
        ))
        val VOCAL = EqualizerPreset("Vocal", listOf(
            EqualizerBand(0, 60, -2f), EqualizerBand(1, 230, 0f),
            EqualizerBand(2, 910, 4f), EqualizerBand(3, 3600, 3f),
            EqualizerBand(4, 14000, -1f)
        ))
        val ALL = listOf(FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL)
    }
}

// ─── Search ───────────────────────────────────────────────────────────────────

data class SearchResult(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

// ─── Download ─────────────────────────────────────────────────────────────────

enum class AudioQuality(val label: String) {
    HIGH("High (FLAC)"),
    NORMAL("Normal (MP3)")
}

data class DownloadTask(
    val trackId: String,
    val trackTitle: String,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus { PENDING, DOWNLOADING, DONE, ERROR }

// ─── App Theme ────────────────────────────────────────────────────────────────

enum class AppTheme(val label: String) {
    BLUE_PURPLE("Сине-фиолетовый"),
    YELLOW_GREEN("Жёлто-зелёный"),
    BLACK_WHITE("Чёрно-белый")
}

// ─── Player State ─────────────────────────────────────────────────────────────

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val speedMode: SpeedMode = SpeedMode.NORMAL,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT
)

enum class RepeatMode { NONE, ONE, ALL }
