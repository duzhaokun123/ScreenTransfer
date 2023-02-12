#include <jni.h>
#include <jni.h>

//
// Created by duzhaokun on 2022/7/14.
//

char* get_linker();

extern "C"
JNIEXPORT jstring JNICALL

JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegExe_getLinker(JNIEnv* env, jobject) {
    return env->NewStringUTF(get_linker());
}

char* get_linker() {
#ifdef __i386__
    auto linker = "/system/bin/linker";
#elif __x86_64__
    auto linker = "/system/bin/linker64";
#elif __arm__
        auto linker = "/system/bin/linker";
#elif __aarch64__
        auto linker = "/system/bin/linker64";
#else
#error unsupport plantfom
#endif
    return const_cast<char*>(linker);
}