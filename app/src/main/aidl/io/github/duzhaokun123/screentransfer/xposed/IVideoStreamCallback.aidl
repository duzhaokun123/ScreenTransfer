package io.github.duzhaokun123.screentransfer.xposed;

interface IVideoStreamCallback {
    void onStream(in byte[] bytes, int flags, long timeUs);
}
