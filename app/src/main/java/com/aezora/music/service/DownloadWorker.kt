package com.aezora.music.service

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.aezora.music.domain.model.Track
import com.aezora.music.domain.model.TrackSource
import com.aezora.music.domain.repository.MusicRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MusicRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TRACK_JSON = "track_json"
        const val KEY_QUALITY_HIGH = "quality_high"
        const val KEY_USE_SAF = "use_saf"
        const val KEY_SAF_URI = "saf_uri"

        fun buildRequest(track: Track, qualityHigh: Boolean): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_TRACK_JSON, Gson().toJson(track))
                .putBoolean(KEY_QUALITY_HIGH, qualityHigh)
                .build()
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("download_${track.id}")
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trackJson = inputData.getString(KEY_TRACK_JSON) ?: return@withContext Result.failure()
        val qualityHigh = inputData.getBoolean(KEY_QUALITY_HIGH, false)
        val track = Gson().fromJson(trackJson, Track::class.java)

        try {
            // Resolve stream URL
            val streamUrl = repository.resolveStreamUrl(track, qualityHigh)
                ?: return@withContext Result.failure()

            // Determine output directory
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Aezora"
            )
            dir.mkdirs()

            val ext = if (qualityHigh) "flac" else "mp3"
            val safeName = "${track.artist} - ${track.title}".replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val file = File(dir, "$safeName.$ext")

            // Download file
            val request = Request.Builder().url(streamUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure()
                val body = response.body ?: return@withContext Result.failure()
                val total = body.contentLength()
                var downloaded = 0L

                FileOutputStream(file).use { out ->
                    val buffer = ByteArray(8192)
                    body.byteStream().use { input ->
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            out.write(buffer, 0, bytes)
                            downloaded += bytes
                            if (total > 0) {
                                val progress = (downloaded * 100 / total).toInt()
                                setProgress(workDataOf("progress" to progress))
                            }
                        }
                    }
                }
            }

            // Update database
            repository.markDownloaded(track.id, file.absolutePath)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
