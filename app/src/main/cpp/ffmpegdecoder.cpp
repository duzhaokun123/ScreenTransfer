#include <jni.h>
#include <cstdlib>
#include <android/log.h>
#include "Decoder.h"
#include <array>
#include <android/bitmap.h>

extern "C" {
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
}

void yuv_rgb(int width, int height, uint8_t* ys, uint8_t* us, uint8_t* vs, uint8_t* rgbs) {
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            auto i = y * width + x;
            auto Y = ys[i];
            auto V = vs[y / 2 * width / 2 + x / 2];
            auto U = us[y / 2 * width / 2 + x / 2];
            auto R = 1.164 * (Y - 16) + 1.596 * (V - 128)                    ;
            auto G = 1.164 * (Y - 16) - 0.813 * (V - 128) - 0.391 * (U - 128);
            auto B = 1.164 * (Y - 16)                     + 2.018 * (U - 128);
            #define CLIP(X) (X > 255 ? 255 : X < 0 ? 0 : X)
            R = CLIP(R);
            G = CLIP(G);
            B = CLIP(B);
            #undef CLIP
            rgbs[i + 0] = static_cast<uint8_t>(R);
            rgbs[i + 1] = static_cast<uint8_t>(G);
            rgbs[i + 2] = static_cast<uint8_t>(B);
        }
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegDecoder_nativeInit(JNIEnv* env,
                                                                           jobject thiz) {
    auto decoder = (Decoder*)malloc(sizeof(Decoder));
    decoder->codec = avcodec_find_decoder(AV_CODEC_ID_HEVC);
    auto c = avcodec_alloc_context3(decoder->codec);
    decoder->c = c;
    avcodec_open2(c, decoder->codec, nullptr);
    auto clazz = env->GetObjectClass(thiz);
    decoder->onNewFrame = env->GetMethodID(clazz, "nativeOnNewFrame","(Landroid/graphics/Bitmap;)V");
    return (jlong)decoder;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegDecoder_nativeRelease(JNIEnv*,
                                                                              jobject,
                                                                              jlong address) {
    auto decoder = (Decoder*)address;
    avcodec_close(decoder->c);
    av_free(decoder->c);
    free((void*)address);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegDecoder_00024Companion_getVersion(
        JNIEnv* env, jobject) {
    return env->NewStringUTF(av_version_info());
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_duzhaokun123_screentransfer_ffmpeg_FFmpegDecoder_nativeWrite(JNIEnv* env,
                                                                            jobject thiz,
                                                                            jlong address,
                                                                            jbyteArray bytes,
                                                                            jint size,
                                                                            jobject bitmap) {
    auto decoder = (Decoder*)address;
    auto b = env->GetByteArrayElements(bytes, JNI_FALSE);
    auto packet = av_packet_alloc();
    packet->size = size;
    auto buf = (uint8_t*)malloc(sizeof(uint8_t) * size);
    for (int i = 0; i < size; ++i) {
        buf[i] = b[i];
    }
    packet->data = buf;
    avcodec_send_packet(decoder->c, packet);
    free(buf);
    av_packet_free(&packet);

    auto frame = av_frame_alloc();
    avcodec_receive_frame(decoder->c, frame);

    if (frame->format != AV_PIX_FMT_NONE) {
        auto width = frame->width;
        auto height = frame->height;
        auto rs = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
        auto rgbs = (uint8_t*)malloc(rs);
        auto rgbFrame = av_frame_alloc();
        av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, (uint8_t*)rgbs, AV_PIX_FMT_RGB24, width, height, 1);
        rgbFrame->width = width;
        rgbFrame->height = height;
        auto sc = sws_getContext(width, height, (AVPixelFormat)frame->format,
                                 width, height, AV_PIX_FMT_RGB24,
                                 SWS_BILINEAR, nullptr, nullptr, nullptr);
        sws_scale(sc, frame->data, frame->linesize, 0, height, rgbFrame->data, rgbFrame->linesize);
        sws_freeContext(sc);
        int32_t* bbuf = 0;
        AndroidBitmap_lockPixels(env, bitmap, (void**)&bbuf);
        for (int i = 0; i < width * height; i++) {
            //bitmap is AABBGGRR
            bbuf[i] = (0xFF << 24)
                    + (rgbs[i * 3 + 0] << 0)
                    + (rgbs[i * 3 + 1] << 8)
                    + (rgbs[i * 3 + 2] << 16);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        env->CallVoidMethod(thiz, decoder->onNewFrame, bitmap);
        free(rgbs);
        av_frame_free(&rgbFrame);
    }

    av_frame_free(&frame);
}
