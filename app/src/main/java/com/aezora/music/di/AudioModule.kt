package com.aezora.music.di

import com.aezora.music.audio.AudioEngine
import com.aezora.music.audio.RubberBandEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioEngine(
        impl: RubberBandEngine
    ): AudioEngine
}
