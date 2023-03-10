package io.github.duzhaokun123.screentransfer.service.display

import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.MotionEvent
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.service.NetService
import io.github.duzhaokun123.screentransfer.service.xposed.utils.Instances
import io.github.duzhaokun123.screentransfer.service.xposed.utils.TipUtil
import io.github.duzhaokun123.screentransfer.utils.ParcelableUtil
import java.util.concurrent.LinkedBlockingQueue

class RemoteDisplay(width: Int, height: Int, densityDpi: Int) {
    companion object {
        const val TAG = "ScrTsf_RD"
        const val DEFAULT_I_FRAME_INTERVAL = 10 // seconds
        const val REPEAT_FRAME_DELAY_US = 100_000L // repeat after 100ms
    }

    var closed = false
    lateinit var virtualDisplay: VirtualDisplay
    val codec: MediaCodec
    val virtualDisplayId: Int
        get() = virtualDisplay.display.displayId
    var videoFrameQueue = LinkedBlockingQueue<ByteArray>()
    val streamCallback = object : NetService.StreamCallback {
        override fun onVideoFrameSenderAvailable(sender: NetService.ByteArraySender) {
            runNewThread {
                while (closed.not()) {
                    val frame = videoFrameQueue.take()
                    sender.send(frame)
                }
            }
        }

        override fun onEvent(bytes: ByteArray) {
            val type = bytes[0]
            val data = bytes.copyOfRange(1, bytes.size)
            when(type) {
                NetService.StreamCallback.EVENT_TYPE_MOTION -> {
                    val event = ParcelableUtil.unmarshall(data, MotionEvent.CREATOR)
                    event.invokeMethod("setDisplayId", args(virtualDisplayId), argTypes(Integer.TYPE))
                    Instances.inputManager.injectInputEvent(event, 0)
                }
            }

        }

        override fun onClose() {
            close()
        }

        override fun getId(): Int {
            return virtualDisplayId
        }
    }

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
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: BufferInfo
                ) {
                    val buffer = codec.getOutputBuffer(index) ?: return
                    val array = ByteArray(buffer.remaining())
                    buffer.get(array)
                    runCatching {
                        videoFrameQueue.put(array)
                        codec.releaseOutputBuffer(index, false)
                    }.onFailure {
                        Log.e(TAG, "onOutputBufferAvailable: ", it)
                        virtualDisplay.release()
                        codec.release()
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
            })
        }
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        virtualDisplay = Instances.displayManager.createVirtualDisplay("ScrTsf${System.currentTimeMillis()}", width, height, densityDpi, codec.createInputSurface(), 1668)
        TipUtil.showToast("display $virtualDisplayId created")
        codec.start()
    }

    fun close() {
        closed = true
        virtualDisplay.release()
        codec.stop()
        codec.release()
    }
}