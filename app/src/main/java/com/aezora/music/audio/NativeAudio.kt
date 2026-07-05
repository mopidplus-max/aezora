package com.aezora.music.audio

object NativeAudio {

    init {
        System.loadLibrary("aezora_audio")
    }

    external fun version(): String

    external fun setTempo(tempo: Float)

    external fun setPitchSemiTones(semitones: Float)
}
