//
// Created by duzha on 2023/2/13.
//

extern "C" {
#include "libavcodec/avcodec.h"
}
#include "jni.h"

#ifndef SCREENTRANSFER_DECODER_H
#define SCREENTRANSFER_DECODER_H

struct Decoder {
    const AVCodec* codec;
    AVCodecContext* c;
    jmethodID onNewFrame;
};

#endif //SCREENTRANSFER_DECODER_H
