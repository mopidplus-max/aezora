package com.aezora.music.domain.repository

import com.aezora.music.data.local.*
import com.aezora.music.data.remote.soundcloud.SoundCloudService
import com.aezora.music.data.remote.yandex.YandexMusicService
import com.aezora.music.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val soundCloudService: SoundCloudService,
    private val yandexMusicService: YandexMusicService
) {
    // ─── Tracks ───────────────────────────────────────────────────────────────

    fun getLikedTracks(): Flow<List<Track>> =
        trackDao.getLikedTracks().map { list -> list.map { it.toDomain() } }

    fun getDownloadedTracks(): Flow<List<Track>> =
        trackDao.getDownloadedTracks().map { list -> list.map { it.toDomain() } }

    suspend fun toggleLike(track: Track) {
        trackDao.insertTrack(track.copy(isLiked = !track.isLiked).toEntity())
    }

    suspend fun cacheTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
    }

    suspend fun markDownloaded(trackId: String, path: String) {
        trackDao.setDownloaded(trackId, true, path)
    }

    suspend fun incrementPlayCount(trackId: String) {
        trackDao.incrementPlayCount(trackId)
    }

    // ─── Playlists ────────────────────────────────────────────────────────────

    fun getUserPlaylists(): Flow<List<Playlist>> =
        playlistDao.getUserPlaylists().map { list -> list.map { it.toDomain() } }

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list -> list.map { it.toDomain() } }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlist = Playlist(
            id = "local_${UUID.randomUUID()}",
            name = name,
            description = description,
            isUserCreated = true,
            source = PlaylistSource.LOCAL
        )
        playlistDao.insertPlaylist(playlist.toEntity())
        return playlist
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        trackDao.insertTrack(track.toEntity())
        val existing = playlistDao.getPlaylistById(playlistId)
        val pos = existing?.tracks?.size ?: 0
        playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, track.id, pos))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun deletePlaylist(playlistId: String) {
        val entity = PlaylistEntity(playlistId, "", "", "", true, "LOCAL", 0)
        playlistDao.deletePlaylist(entity)
    }

    suspend fun renamePlaylist(id: String, name: String, desc: String) {
        playlistDao.updatePlaylist(id, name, desc)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val scTracks = try { soundCloudService.search(query) } catch (e: Exception) { emptyList() }
        val ymTracks = if (yandexMusicService.hasToken()) {
            try { yandexMusicService.search(query) } catch (e: Exception) { emptyList() }
        } else emptyList()

        // Merge: interleave results
        val merged = mutableListOf<Track>()
        val maxLen = maxOf(scTracks.size, ymTracks.size)
        for (i in 0 until maxLen) {
            if (i < scTracks.size) merged.add(scTracks[i])
            if (i < ymTracks.size) merged.add(ymTracks[i])
        }
        SearchResult(tracks = merged)
    }

    // ─── Discover / Home ──────────────────────────────────────────────────────

    suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        val sc = try { soundCloudService.getTrending() } catch (e: Exception) { emptyList() }
        val ym = if (yandexMusicService.hasToken()) {
            try { yandexMusicService.getLanding() } catch (e: Exception) { emptyList() }
        } else emptyList()
        (sc + ym).shuffled().take(30)
    }

    suspend fun getYandexPlaylists(): List<Playlist> {
        if (!yandexMusicService.hasToken()) return emptyList()
        return try { yandexMusicService.getUserPlaylists() } catch (e: Exception) { emptyList() }
    }

    // ─── Stream URL Resolution ────────────────────────────────────────────────

    suspend fun resolveStreamUrl(track: Track, qualityHigh: Boolean = false): String? {
        if (track.localPath != null) return track.localPath
        return when (track.source) {
            TrackSource.SOUNDCLOUD -> soundCloudService.resolveStreamUrl(track.streamUrl)
            TrackSource.YANDEX -> yandexMusicService.getStreamUrl(track.id, if (qualityHigh) "flac" else "mp3_192")
            TrackSource.LOCAL -> track.localPath
        }
    }

    // ─── Yandex liked tracks ──────────────────────────────────────────────────

    suspend fun getYandexLikedTracks(): List<Track> {
        if (!yandexMusicService.hasToken()) return emptyList()
        return try { yandexMusicService.getLikedTracks() } catch (e: Exception) { emptyList() }
    }
}
