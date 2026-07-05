package com.aezora.music.audio

import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class PitchAudioProcessor : BaseAudioProcessor() {

    var semitones: Float = 0f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer).flip()
    }

    override fun onFlush() {}

    override fun onReset() {
        semitones = 0f
    }
}
