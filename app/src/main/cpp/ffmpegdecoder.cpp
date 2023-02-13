#include <jni.h>
#include <cstdlib>
#include "Decoder.h"

extern "C"
JNIEXPORT jlong JNICALL

JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegDecoder_nativeInit(JNIEnv*, jobject) {
    return (jlong)malloc(sizeof(Decoder));
}