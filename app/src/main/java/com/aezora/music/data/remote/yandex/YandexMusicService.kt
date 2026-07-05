package com.aezora.music.data.remote.yandex

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import com.aezora.music.domain.model.Playlist
import com.aezora.music.domain.model.PlaylistSource
import com.aezora.music.domain.model.Track
import com.aezora.music.domain.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yandex Music API client using the unofficial API (same as yandex-music-api Python lib).
 * Requires a valid OAuth token set by the user.
 */
@Singleton
class YandexMusicService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var token: String = ""
    private var userId: String = ""

    companion object {
        private const val BASE_URL = "https://api.music.yandex.net"
        private const val DEVICE = "os=Android; os_version=12; manufacturer=Google; model=Pixel; clid=; device_id=random; uuid=random"
    }

    fun setToken(t: String) {
        token = t
    }

    fun hasToken() = token.isNotBlank()

    // ─── Auth / User ──────────────────────────────────────────────────────────

    suspend fun fetchUserId(): Boolean = withContext(Dispatchers.IO) {
        val json = get("/account/status") ?: return@withContext false
        val account = JSONObject(json).optJSONObject("result")?.optJSONObject("account")
        userId = account?.optString("uid") ?: return@withContext false
        userId.isNotBlank()
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = get("/search?text=$encoded&type=track&page=0") ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONObject("result") ?: return@withContext emptyList()
        val tracks = result.optJSONObject("tracks")?.optJSONArray("results") ?: return@withContext emptyList()
        parseTracks(tracks)
    }

    // ─── Landing / My Wave ────────────────────────────────────────────────────

    suspend fun getMyWave(): List<Track> = withContext(Dispatchers.IO) {
        val json = get("/users/$userId/stations/user:onyourwave/tracks") ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONObject("result") ?: return@withContext emptyList()
        val sequence = result.optJSONArray("sequence") ?: return@withContext emptyList()
        val tracks = JSONArray()
        for (i in 0 until sequence.length()) {
            val item = sequence.optJSONObject(i) ?: continue
            val track = item.optJSONObject("track") ?: continue
            tracks.put(track)
        }
        parseTracks(tracks)
    }

    suspend fun getLanding(): List<Track> = withContext(Dispatchers.IO) {
        val json = get("/landing3?blocks=personal-playlists,promotions,new-releases,new-playlists,mixes,chart,podcasts")
            ?: return@withContext emptyList()
        // Return chart tracks as a simple list
        val result = JSONObject(json).optJSONObject("result") ?: return@withContext emptyList()
        val blocks = result.optJSONArray("blocks") ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            if (block.optString("type") == "chart") {
                val entities = block.optJSONArray("entities") ?: continue
                for (j in 0 until entities.length()) {
                    val entity = entities.optJSONObject(j) ?: continue
                    val data = entity.optJSONObject("data") ?: continue
                    val track = data.optJSONObject("track") ?: continue
                    parseTrack(track)?.let { tracks.add(it) }
                }
            }
        }
        tracks
    }

    // ─── User Playlists ───────────────────────────────────────────────────────

    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) fetchUserId()
        val json = get("/users/$userId/playlists/list") ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONArray("result") ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        for (i in 0 until result.length()) {
            val obj = result.optJSONObject(i) ?: continue
            parsePlaylist(obj)?.let { playlists.add(it) }
        }
        playlists
    }

    suspend fun getPlaylistTracks(kind: Int): List<Track> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) fetchUserId()
        val json = get("/users/$userId/playlists/$kind") ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONObject("result") ?: return@withContext emptyList()
        val tracks = result.optJSONArray("tracks") ?: return@withContext emptyList()
        parseTracks(tracks)
    }

    // ─── Liked tracks ─────────────────────────────────────────────────────────

    suspend fun getLikedTracks(): List<Track> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) fetchUserId()
        val json = get("/users/$userId/likes/tracks") ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONObject("result") ?: return@withContext emptyList()
        val library = result.optJSONObject("library") ?: return@withContext emptyList()
        val tracks = library.optJSONArray("tracks") ?: return@withContext emptyList()

        // These are just track ids, need to fetch details
        val ids = mutableListOf<String>()
        for (i in 0 until tracks.length()) {
            val t = tracks.optJSONObject(i) ?: continue
            ids.add(t.optString("id"))
        }
        if (ids.isEmpty()) return@withContext emptyList()
        fetchTracksByIds(ids)
    }

    private suspend fun fetchTracksByIds(ids: List<String>): List<Track> = withContext(Dispatchers.IO) {
        val body = "track-ids=${ids.joinToString(",")}"
        val json = post("/tracks", body) ?: return@withContext emptyList()
        val result = JSONObject(json).optJSONArray("result") ?: return@withContext emptyList()
        parseTracks(result)
    }

    // ─── Stream URL ───────────────────────────────────────────────────────────

    suspend fun getStreamUrl(trackId: String, quality: String = "mp3_192"): String? = withContext(Dispatchers.IO) {
        val rawId = trackId.removePrefix("ym_")
        val json = get("/tracks/$rawId/download-info") ?: return@withContext null
        val result = JSONObject(json).optJSONArray("result") ?: return@withContext null

        // Find best quality
        var best: JSONObject? = null
        for (i in 0 until result.length()) {
            val info = result.optJSONObject(i) ?: continue
            val codec = info.optString("codec")
            val bitrateInKbps = info.optInt("bitrateInKbps", 0)
            if (quality.contains("flac") && codec == "flac") { best = info; break }
            if (quality.contains("mp3") && codec == "mp3" && bitrateInKbps >= 192) { best = info; break }
            if (best == null) best = info
        }

        best?.let { resolveDownloadInfo(it) }
    }

    private suspend fun resolveDownloadInfo(info: JSONObject): String? = withContext(Dispatchers.IO) {
        val downloadInfoUrl = info.optString("downloadInfoUrl").takeIf { it.isNotBlank() }
            ?: return@withContext null
        val xml = rawGet(downloadInfoUrl) ?: return@withContext null

        // Parse XML manually (avoid dependency)
        fun extractTag(tag: String): String {
            val start = xml.indexOf("<$tag>").takeIf { it >= 0 }?.plus(tag.length + 2) ?: return ""
            val end = xml.indexOf("</$tag>", start).takeIf { it >= 0 } ?: return ""
            return xml.substring(start, end)
        }

        val host = extractTag("host")
        val path = extractTag("path")
        val ts = extractTag("ts")
        val s = extractTag("s")
        val sign = md5("XGRlBW9FXlekgbPrRHuSiA${path.substring(1)}$s")

        "https://$host/get-mp3/$sign/$ts$path"
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private fun parseTracks(arr: JSONArray): List<Track> {
        val list = mutableListOf<Track>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            parseTrack(obj)?.let { list.add(it) }
        }
        return list
    }

    private fun parseTrack(obj: JSONObject): Track? {
        return try {
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return null
            val durationMs = obj.optLong("durationMs")
            val artists = obj.optJSONArray("artists")
            val artist = (0 until (artists?.length() ?: 0))
                .mapNotNull { artists?.optJSONObject(it)?.optString("name") }
                .joinToString(", ")
                .takeIf { it.isNotBlank() } ?: "Unknown"
            val albums = obj.optJSONArray("albums")
            val album = albums?.optJSONObject(0)?.optString("title") ?: ""
            val coverUri = albums?.optJSONObject(0)?.optString("coverUri")
                ?.replace("%%", "400x400") ?: ""
            val artworkUrl = if (coverUri.startsWith("http")) coverUri else "https://$coverUri"

            Track(
                id = "ym_$id",
                title = title,
                artist = artist,
                album = album,
                artworkUrl = artworkUrl,
                duration = durationMs,
                source = TrackSource.YANDEX,
                streamUrl = "", // resolve on demand
                isLiked = false,
                isDownloaded = false,
                hasDrm = false,
                genre = ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePlaylist(obj: JSONObject): Playlist? {
        return try {
            val kind = obj.optInt("kind")
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return null
            val desc = obj.optString("description")
            val cover = obj.optJSONObject("cover")?.optString("uri")
                ?.replace("%%", "400x400") ?: ""
            val artworkUrl = if (cover.startsWith("http")) cover else if (cover.isNotBlank()) "https://$cover" else ""
            Playlist(
                id = "ym_playlist_$kind",
                name = title,
                description = desc,
                artworkUrl = artworkUrl,
                tracks = emptyList(),
                isUserCreated = false,
                source = PlaylistSource.YANDEX
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private fun get(path: String): String? = rawGet("$BASE_URL$path")

    private fun rawGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "OAuth $token")
                .addHeader("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                .addHeader("User-Agent", "Yandex-Music-Android")
                .addHeader("Accept-Language", "ru")
                .build()
            okHttpClient.newCall(req).execute().use { it.body?.string() }
        } catch (e: Exception) {
            null
        }
    }
    private fun post(path: String, body: String): String? {
        return try {
            val rb = body.toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val req = Request.Builder()
                .url("$BASE_URL$path")
                .post(rb)
                .addHeader("Authorization", "OAuth $token")
                .addHeader("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                .build()
            okHttpClient.newCall(req).execute().use { it.body?.string() }
        } catch (e: Exception) {
            null
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }
}
