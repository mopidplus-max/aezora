#include <jni.h>
#include <SoundTouch.h>
#include <vector>

using namespace soundtouch;

static SoundTouch engine;

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM*, void*) {
    engine.setSampleRate(44100);
    engine.setChannels(2);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_setTempo(
        JNIEnv*, jobject, jfloat tempo) {
    engine.setTempo(tempo);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_setPitchSemiTones(
        JNIEnv*, jobject, jfloat pitch) {
    engine.setPitchSemiTones(pitch);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_clear(
        JNIEnv*, jobject) {
    engine.clear();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_putSamples(
        JNIEnv* env,
        jobject,
        jshortArray samples,
        jint numSamples) {

    jshort* ptr = env->GetShortArrayElements(samples, nullptr);

    engine.putSamples(
        reinterpret_cast<SAMPLETYPE*>(ptr),
        static_cast<uint>(numSamples)
    );

    env->ReleaseShortArrayElements(samples, ptr, JNI_ABORT);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_aezora_music_audio_NativeAudio_receiveSamples(
        JNIEnv* env,
        jobject,
        jshortArray out,
        jint maxSamples) {

    jshort* ptr = env->GetShortArrayElements(out, nullptr);

    uint received = engine.receiveSamples(
        reinterpret_cast<SAMPLETYPE*>(ptr),
        static_cast<uint>(maxSamples)
    );

    env->ReleaseShortArrayElements(out, ptr, 0);

    return (jint)received;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aezora_music_audio_NativeAudio_version(
        JNIEnv* env,
        jobject) {
    return env->NewStringUTF(SOUNDTOUCH_VERSION);
}
