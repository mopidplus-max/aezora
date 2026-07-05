package com.aezora.music.data.remote.soundcloud

import com.aezora.music.domain.model.Track
import com.aezora.music.domain.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SoundCloud integration via the public API only.
 * No yt-dlp. Stream URLs are resolved through SoundCloud transcodings.
 */
@Singleton
class SoundCloudService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val BASE_URL = "https://api-v2.soundcloud.com"
        private const val CLIENT_ID = "yNSW5UvBmb1A5j7qPUtIMuB9Itx3jsOC"
        private const val LIMIT = 20
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/search/tracks" +
            "?q=${URLEncoder.encode(query, "UTF-8")}" +
            "&client_id=$CLIENT_ID" +
            "&limit=$LIMIT"

        val json = get(url) ?: return@withContext emptyList()
        val collection = JSONObject(json).optJSONArray("collection") ?: return@withContext emptyList()
        parseTracks(collection)
    }

    suspend fun getTrending(genre: String = "all-music"): List<Track> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/charts" +
            "?kind=trending&genre=soundcloud:genres:$genre" +
            "&client_id=$CLIENT_ID&limit=$LIMIT"

        val json = get(url) ?: return@withContext emptyList()
        val collection = JSONObject(json).optJSONArray("collection") ?: return@withContext emptyList()

        // Charts wrap tracks in {"track": {...}} objects
        val tracks = JSONArray()
        for (i in 0 until collection.length()) {
            val item = collection.optJSONObject(i) ?: continue
            val track = item.optJSONObject("track") ?: continue
            tracks.put(track)
        }
        parseTracks(tracks)
    }

    /**
     * Resolve stream URL for playback.
     * Accepts either:
     * - SoundCloud permalink URL (https://soundcloud.com/...)
     * - SoundCloud API transcoding URL (https://api-v2.soundcloud.com/...)
     */
    suspend fun resolveStreamUrl(trackPermalinkOrApiUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            if (trackPermalinkOrApiUrl.contains("api-v2.soundcloud.com")) {
                val direct = get("$trackPermalinkOrApiUrl?client_id=$CLIENT_ID") ?: return@withContext null
                val directUrl = JSONObject(direct).optString("url")
                if (directUrl.startsWith("http")) return@withContext directUrl
            }

            resolveFromPermalink(trackPermalinkOrApiUrl)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveFromPermalink(permalink: String): String? = withContext(Dispatchers.IO) {
        try {
            val resolveUrl =
                "$BASE_URL/resolve?url=${URLEncoder.encode(permalink, "UTF-8")}&client_id=$CLIENT_ID"

            val trackJson = get(resolveUrl) ?: return@withContext null
            val trackObj = JSONObject(trackJson)

            extractDirectStreamUrl(trackObj)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDirectStreamUrl(trackObj: JSONObject): String? {
        val media = trackObj.optJSONObject("media") ?: return null
        val transcodings = media.optJSONArray("transcodings") ?: return null

        var fallbackApiUrl: String? = null

        for (i in 0 until transcodings.length()) {
            val t = transcodings.optJSONObject(i) ?: continue
            val apiUrl = t.optString("url")
            if (apiUrl.isBlank()) continue

            val format = t.optJSONObject("format")
            val protocol = format?.optString("protocol").orEmpty()

            if (protocol == "progressive") {
                val streamJson = get("$apiUrl?client_id=$CLIENT_ID") ?: continue
                val url = JSONObject(streamJson).optString("url")
                if (url.startsWith("http")) return url
            }

            if (fallbackApiUrl == null) fallbackApiUrl = apiUrl
        }

        if (fallbackApiUrl != null) {
            val streamJson = get("$fallbackApiUrl?client_id=$CLIENT_ID") ?: return null
            val url = JSONObject(streamJson).optString("url")
            if (url.startsWith("http")) return url
        }

        return null
    }

    private fun parseTracks(collection: JSONArray): List<Track> {
        val tracks = mutableListOf<Track>()
        for (i in 0 until collection.length()) {
            val obj = collection.optJSONObject(i) ?: continue
            val track = parseTrack(obj) ?: continue
            if (track.hasDrm) continue
            tracks.add(track)
        }
        return tracks
    }

    private fun parseTrack(obj: JSONObject): Track? {
        return try {
            val id = obj.optLong("id").toString()
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return null
            val user = obj.optJSONObject("user")
            val artist = user?.optString("username").takeIf { !it.isNullOrBlank() } ?: "Unknown"
            val duration = obj.optLong("duration")
            val permalink = obj.optString("permalink_url")
            val artwork = obj.optString("artwork_url").replace("large", "t500x500")
            val genre = obj.optString("genre")
            val playCount = obj.optInt("playback_count")
            val policy = obj.optString("policy", "")

            val hasDrm = policy.equals("SNIP", ignoreCase = true) ||
                policy.equals("BLOCK", ignoreCase = true)

            val streamApiUrl = extractTranscodingApiUrl(obj) ?: permalink

            Track(
                id = "sc_$id",
                title = title,
                artist = artist,
                album = "",
                artworkUrl = artwork,
                duration = duration,
                source = TrackSource.SOUNDCLOUD,
                streamUrl = streamApiUrl,
                isLiked = false,
                isDownloaded = false,
                hasDrm = hasDrm,
                genre = genre,
                playCount = playCount
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTranscodingApiUrl(obj: JSONObject): String? {
        val media = obj.optJSONObject("media") ?: return null
        val transcodings = media.optJSONArray("transcodings") ?: return null

        var fallbackApiUrl: String? = null

        for (i in 0 until transcodings.length()) {
            val t = transcodings.optJSONObject(i) ?: continue
            val apiUrl = t.optString("url")
            if (apiUrl.isBlank()) continue

            val format = t.optJSONObject("format")
            val protocol = format?.optString("protocol").orEmpty()

            if (protocol == "progressive") return apiUrl
            if (fallbackApiUrl == null) fallbackApiUrl = apiUrl
        }

        return fallbackApiUrl
    }

    private fun get(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .header("Origin", "https://soundcloud.com")
                .header("Referer", "https://soundcloud.com/")
                .build()

            okHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
