package com.aezora.music.audio

import com.aezora.music.domain.model.SpeedMode

interface AudioEngine {
    fun applyPreset(mode: SpeedMode)
}
