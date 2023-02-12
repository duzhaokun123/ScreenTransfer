package io.github.duzhaokun123.screentransfer.xposed;

import io.github.duzhaokun123.screentransfer.xposed.IVideoStreamCallback;

interface IScrTsfManager {
    String getVersionName();

    int getVersionCode();

    void createWindow(IVideoStreamCallback iVideoStreamCallback);

    long getBuildTime();
}