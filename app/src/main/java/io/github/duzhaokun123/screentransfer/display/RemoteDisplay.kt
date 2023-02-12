package io.github.duzhaokun123.screentransfer.display

import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.xposed.IVideoStreamCallback
import io.github.duzhaokun123.screentransfer.xposed.utils.Instances
import java.io.File

class RemoteDisplay(width: Int, height: Int, densityDpi: Int, iVideoStreamCallback: IVideoStreamCallback) {
    companion object {
        const val TAG = "ScrTsf_RD"
        const val DEFAULT_I_FRAME_INTERVAL = 10 // seconds
        const val REPEAT_FRAME_DELAY_US = 100_000L // repeat after 100ms
    }

    lateinit var virtualDisplay: VirtualDisplay
    val codec: MediaCodec

    init {
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 10000)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL)
        mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, REPEAT_FRAME_DELAY_US)
        runNewThread {
            codec.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: BufferInfo
                ) {
                    val buffer = codec.getOutputBuffer(index) ?: return
                    val array = ByteArray(buffer.remaining())
                    buffer.get(array)
                    runCatching {
                        iVideoStreamCallback.onStream(array, info.flags, info.presentationTimeUs)
                        codec.releaseOutputBuffer(index, false)
                    }.onFailure {
                        virtualDisplay.release()
                        codec.release()
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {

                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {

                }
            })
        }
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        virtualDisplay = Instances.displayManager.createVirtualDisplay("ScrTsf${System.currentTimeMillis()}", width, height, densityDpi, codec.createInputSurface(), 1668)
        Log.d(TAG, "init: ${virtualDisplay.display.displayId}")
        codec.start()

    }
}