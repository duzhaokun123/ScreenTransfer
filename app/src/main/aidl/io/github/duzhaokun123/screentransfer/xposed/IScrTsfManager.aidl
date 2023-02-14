package io.github.duzhaokun123.screentransfer.xposed;

import io.github.duzhaokun123.screentransfer.xposed.IStreamCallback;

interface IScrTsfManager {
    String getVersionName();

    int getVersionCode();

    long getBuildTime();

    IStreamCallback createDisplay(int width, int height, int densityDpi);
}