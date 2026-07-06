package com.aezora.music.audio

object NativeAudio {

    init {
        System.loadLibrary("aezora_audio")
    }

    external fun version(): String

    external fun setTempo(tempo: Float)

    external fun setPitchSemiTones(semitones: Float)

    external fun clear()

    external fun putSamples(
        samples: ShortArray,
        numSamples: Int
    )

    external fun receiveSamples(
        out: ShortArray,
        maxSamples: Int
    ): Int
}
