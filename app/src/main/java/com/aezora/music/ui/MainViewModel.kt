package com.aezora.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.work.WorkManager
import com.aezora.music.data.local.PreferencesManager
import com.aezora.music.data.remote.yandex.YandexMusicService
import com.aezora.music.domain.model.*
import com.aezora.music.domain.repository.MusicRepository
import com.aezora.music.service.DownloadWorker
import com.aezora.music.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val prefsManager: PreferencesManager,
    private val yandexService: YandexMusicService,
    private val workManager: WorkManager
) : ViewModel() {

    // ─── Player State ─────────────────────────────────────────────────────────
    val playerState = playerController.state

    // ─── UI State ─────────────────────────────────────────────────────────────
    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState = _searchState.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState = _libraryState.asStateFlow()

    val appTheme = prefsManager.appTheme.stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.BLUE_PURPLE)
    val downloadQuality = prefsManager.downloadQuality.stateIn(viewModelScope, SharingStarted.Eagerly, AudioQuality.NORMAL)
    val streamQuality = prefsManager.streamQuality.stateIn(viewModelScope, SharingStarted.Eagerly, AudioQuality.NORMAL)
    val speedMode = prefsManager.speedMode.stateIn(viewModelScope, SharingStarted.Eagerly, SpeedMode.NORMAL)
    val persistSpeed = prefsManager.persistSpeed.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── Init ─────────────────────────────────────────────────────────────────
    init {
        loadYandexToken()
        loadLibrary()
        loadHome()
    }

    private fun loadYandexToken() {
        viewModelScope.launch {
            prefsManager.yandexToken.collect { token ->
                if (token.isNotBlank()) {
                    yandexService.setToken(token)
                    yandexService.fetchUserId()
                }
            }
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            repository.getLikedTracks().collect { tracks ->
                _libraryState.update { it.copy(likedTracks = tracks) }
            }
        }
        viewModelScope.launch {
            repository.getDownloadedTracks().collect { tracks ->
                _libraryState.update { it.copy(downloadedTracks = tracks) }
            }
        }
        viewModelScope.launch {
            repository.getUserPlaylists().collect { playlists ->
                _libraryState.update { it.copy(userPlaylists = playlists) }
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true) }
            try {
                val trending = repository.getTrending()
                val ymPlaylists = repository.getYandexPlaylists()
                _homeState.update {
                    it.copy(trendingTracks = trending, yandexPlaylists = ymPlaylists, isLoading = false)
                }
            } catch (e: Exception) {
                _homeState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.update { it.copy(results = SearchResult(), query = "") }
            return
        }
        viewModelScope.launch {
            _searchState.update { it.copy(isLoading = true, query = query) }
            try {
                val results = repository.search(query)
                _searchState.update { it.copy(results = results, isLoading = false) }
            } catch (e: Exception) {
                _searchState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Playback ─────────────────────────────────────────────────────────────
    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        viewModelScope.launch {
            repository.cacheTrack(track)
            repository.incrementPlayCount(track.id)

            // Resolve stream URL if needed
            val highQuality = streamQuality.value == AudioQuality.HIGH
            val resolvedUrl = repository.resolveStreamUrl(track, highQuality)
            val readyTrack = if (resolvedUrl != null) track.copy(streamUrl = resolvedUrl) else track

            val readyQueue = queue.toMutableList()
            val idx = readyQueue.indexOfFirst { it.id == track.id }
            if (idx >= 0) readyQueue[idx] = readyTrack

            playerController.play(readyQueue, maxOf(0, idx))

            // Apply persisted speed
            if (persistSpeed.value) {
                playerController.setSpeedMode(speedMode.value)
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(ms: Long) = playerController.seekTo(ms)
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun addToQueue(track: Track) = playerController.addToQueue(track)

    fun setSpeedMode(mode: SpeedMode) {
        playerController.setSpeedMode(mode)
        viewModelScope.launch { prefsManager.setSpeedMode(mode) }
    }

    fun setRepeatMode(mode: RepeatMode) = playerController.setRepeatMode(mode)
    fun toggleShuffle() = playerController.toggleShuffle()

    fun applyEqPreset(preset: EqualizerPreset) {
        playerController.applyEqualizerPreset(preset)
        viewModelScope.launch { prefsManager.setEqPreset(preset.name) }
    }

    // ─── Likes ────────────────────────────────────────────────────────────────
    fun toggleLike(track: Track) {
        viewModelScope.launch { repository.toggleLike(track) }
    }

    // ─── Playlists ────────────────────────────────────────────────────────────
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch { repository.createPlaylist(name, description) }
    }

    fun addToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) }
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackId) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    // ─── Downloads ────────────────────────────────────────────────────────────
    fun downloadTrack(track: Track) {
        val highQuality = downloadQuality.value == AudioQuality.HIGH
        val request = DownloadWorker.buildRequest(track, highQuality)
        workManager.enqueue(request)
    }

    fun downloadPlaylist(playlist: Playlist) {
        val highQuality = downloadQuality.value == AudioQuality.HIGH
        playlist.tracks.forEach { track ->
            workManager.enqueue(DownloadWorker.buildRequest(track, highQuality))
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────
    fun setYandexToken(token: String) {
        viewModelScope.launch {
            prefsManager.setYandexToken(token)
            yandexService.setToken(token)
            yandexService.fetchUserId()
            loadHome()
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { prefsManager.setAppTheme(theme) }
    }

    fun setDownloadQuality(q: AudioQuality) {
        viewModelScope.launch { prefsManager.setDownloadQuality(q) }
    }

    fun setStreamQuality(q: AudioQuality) {
        viewModelScope.launch { prefsManager.setStreamQuality(q) }
    }

    fun setPersistSpeed(persist: Boolean) {
        viewModelScope.launch { prefsManager.setPersistSpeed(persist) }
    }

    fun loadYandexLikedTracks() {
        viewModelScope.launch {
            val tracks = repository.getYandexLikedTracks()
            _libraryState.update { it.copy(yandexLikedTracks = tracks) }
        }
    }
}

// ─── UI State data classes ────────────────────────────────────────────────────
data class HomeUiState(
    val trendingTracks: List<Track> = emptyList(),
    val yandexPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchUiState(
    val query: String = "",
    val results: SearchResult = SearchResult(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LibraryUiState(
    val likedTracks: List<Track> = emptyList(),
    val downloadedTracks: List<Track> = emptyList(),
    val userPlaylists: List<Playlist> = emptyList(),
    val yandexLikedTracks: List<Track> = emptyList()
)
