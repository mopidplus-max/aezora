package com.aezora.music.audio

import com.aezora.music.domain.model.SpeedMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RubberBandEngine @Inject constructor() : AudioEngine {

    override fun applyPreset(mode: SpeedMode) {
        // TODO: Rubber Band implementation
    }
}
