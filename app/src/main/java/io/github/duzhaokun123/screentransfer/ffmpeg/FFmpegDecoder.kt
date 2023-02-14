package io.github.duzhaokun123.screentransfer.ffmpeg

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

class FFmpegDecoder(val width: Int, val height: Int, private val onNewFrame: ((frame: Bitmap) -> Unit)) {
    companion object {
        const val TAG = "FFmpegDecoder"

        init {
            System.loadLibrary("ffmpegdecoder")
        }

        external fun getVersion(): String
    }

    val address = nativeInit()

    fun write(bytes: ByteArray) {
        nativeWrite(address, bytes, bytes.size, Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), width, height)
    }

    private external fun nativeInit(): Long
    private external fun nativeRelease(address: Long)
    private external fun nativeWrite(address: Long, bytes: ByteArray, size: Int, bitmap: Bitmap, targetWidth: Int, targetHeight: Int)

    fun release() {
        nativeRelease(address)
    }

    fun nativeOnNewFrame(bitmap: Bitmap) {
        onNewFrame(bitmap)
    }
}