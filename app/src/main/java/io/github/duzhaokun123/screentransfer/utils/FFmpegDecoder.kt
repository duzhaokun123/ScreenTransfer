package io.github.duzhaokun123.screentransfer.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.graphics.toColor
import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.application
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegExe
import java.io.File
import java.io.OutputStream

class FFmpegDecoder(surface: SurfaceHolder, width: Int, height: Int) : OutputStream() {
    companion object {
        const val TAG = "FFmpegDecoder"
    }

    val process = FFmpegExe.commandLine("-i", "pipe:", "-f", "rawvideo", "-pix_fmt", "rgb24", "-probesize", "32", "pipe:")

    init {
        runNewThread {
            process.errorStream.reader().forEachLine {
                Log.v("FFmpegDecoder", "stderr: $it")
            }
        }
        runNewThread {
            val input = process.inputStream.buffered()
            val buf = ByteArray(width * height * 3)
            while (true) {
                input.read(buf, 0, buf.size)
                val pixels = IntArray(width * height)
                pixels.forEachIndexed { i, _ ->
                    val r = buf[i * 3 + 0].toInt()
                    val g = buf[i * 3 + 1].toInt()
                    val b = buf[i * 3 + 2].toInt()
                    pixels[i] = Color.rgb(r, g, b)
                }
                val bitmap = Bitmap.createBitmap(pixels, width, height,Bitmap.Config.ARGB_8888)
                val canvas = surface.lockHardwareCanvas()
                canvas.drawBitmap(bitmap, 0F, 0F, null)
                surface.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun write(b: Int) {
        process.outputStream.write(b)
    }

    override fun write(b: ByteArray?) {
        process.outputStream.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        process.outputStream.write(b, off, len)
    }

    override fun flush() {
        process.outputStream.flush()
    }

    override fun close() {
        process.outputStream.close()
    }
}