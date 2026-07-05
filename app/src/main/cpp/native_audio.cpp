#include <jni.h>
#include <SoundTouch.h>

using namespace soundtouch;

static SoundTouch engine;

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void*) {
    engine.setSampleRate(44100);
    engine.setChannels(2);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_setTempo(
        JNIEnv*,
        jobject,
        jfloat tempo) {
    engine.setTempo(tempo);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aezora_music_audio_NativeAudio_setPitchSemiTones(
        JNIEnv*,
        jobject,
        jfloat pitch) {
    engine.setPitchSemiTones(pitch);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aezora_music_audio_NativeAudio_version(
        JNIEnv* env,
        jobject) {
    return env->NewStringUTF(SOUNDTOUCH_VERSION);
}
