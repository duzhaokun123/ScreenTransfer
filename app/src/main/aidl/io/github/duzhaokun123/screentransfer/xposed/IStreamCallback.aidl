package io.github.duzhaokun123.screentransfer.xposed;

import io.github.duzhaokun123.screentransfer.xposed.IByteArraySender;

interface IStreamCallback {
    void onVideoFrameSenderAvailable(IByteArraySender sender);
    void onEvent(in byte[] event);
    void onClose();
    int getId();
}