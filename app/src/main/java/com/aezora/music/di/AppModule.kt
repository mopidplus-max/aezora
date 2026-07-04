package com.aezora.music.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import androidx.work.WorkManager
import com.aezora.music.data.local.AezoraDatabase
import com.aezora.music.data.local.PlaylistDao
import com.aezora.music.data.local.TrackDao
import com.aezora.music.data.remote.soundcloud.SoundCloudService
import com.aezora.music.data.remote.yandex.YandexMusicService
import com.aezora.music.service.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AezoraDatabase =
        Room.databaseBuilder(ctx, AezoraDatabase::class.java, "aezora.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTrackDao(db: AezoraDatabase): TrackDao = db.trackDao()
    @Provides fun providePlaylistDao(db: AezoraDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideSoundCloudService(ok: OkHttpClient) = SoundCloudService(ok)

    @Provides
    @Singleton
    fun provideYandexMusicService(ok: OkHttpClient) = YandexMusicService(ok)

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun providePlayerController(@ApplicationContext ctx: Context) = PlayerController(ctx)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager =
        WorkManager.getInstance(ctx)
}
