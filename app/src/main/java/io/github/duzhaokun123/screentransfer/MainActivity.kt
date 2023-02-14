package io.github.duzhaokun123.screentransfer

import android.annotation.SuppressLint
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.onFailure
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.androidapptemplate.utils.runIOCatching
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.screentransfer.databinding.ActivityMainBinding
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegDecoder
import io.github.duzhaokun123.screentransfer.service.xposed.ScrTsfManagerHelper
import io.github.duzhaokun123.screentransfer.utils.ParcelableUtil
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.LinkedBlockingDeque


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::class.java, Config.NO_BACK) {
    var client: HttpClient = HttpClient(CIO) { install(WebSockets) }
    var id: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initViews() {
        super.initViews()
        baseBinding.btnPing.setOnClickListener {
            runIOCatching {
                val sendTime = System.currentTimeMillis()
                TipUtil.showTip(this@MainActivity, client.get("http://${baseBinding.etIp.text}:${baseBinding.etPort.text}/ping").bodyAsText() + " ${System.currentTimeMillis() - sendTime}")
            }.onFailure {  t ->
                TipUtil.showTip(this@MainActivity, t)
            }
        }
        baseBinding.btnNewDisplay.setOnClickListener {
            runIOCatching {
                val response = client.get("http://${baseBinding.etIp.text}:${baseBinding.etPort.text}/newdisplay") {
                    parameter("width", "600")
                    parameter("height", "800")
                    parameter("densityDpi", "200")
                }
                if (response.status != HttpStatusCode.OK) {
                    TipUtil.showTip(this@MainActivity, "${response.status}: ${response.bodyAsText()}")
                } else {
                    id = response.bodyAsText()
                    runMain {
                        baseBinding.tvId.text = "id: $id"
                    }
                    val fFmpegDecoder = FFmpegDecoder(600, 800) {
                        val canvas = baseBinding.surface.holder.lockHardwareCanvas()
                        canvas.drawBitmap(it, 0F, 0F, null)
                        baseBinding.surface.holder.unlockCanvasAndPost(canvas)
                    }
                        client.webSocket(host = baseBinding.etIp.text.toString(), port = baseBinding.etPort.text.toString().toInt(), path = "/video/$id") {
                            val f = incoming.receive() as? Frame.Text ?: return@webSocket
                            TipUtil.showTip(this@MainActivity, f.readText())
                            for (frame in incoming) {
                                frame as? Frame.Binary ?: continue
                                fFmpegDecoder.write(frame.readBytes())
                            }
                        }
                }
            }.onFailure {  t ->
                TipUtil.showTip(this@MainActivity, t)
            }
        }
        baseBinding.btn4.setOnClickListener {
            val eventChannel = Channel<ByteArray>()
            runIOCatching {
                client.webSocket(host = baseBinding.etIp.text.toString(), port = baseBinding.etPort.text.toString().toInt(), path = "/event/$id") {
                    val f = incoming.receive() as? Frame.Text ?: return@webSocket
                    TipUtil.showTip(this@MainActivity, f.readText())
                    while (true) {
                        send(eventChannel.receive())
                    }
                }
            }
            baseBinding.surface.setOnTouchListener { _, event ->
                val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(event.pointerCount)
                val pointerProperties: Array<MotionEvent.PointerProperties?> =
                    arrayOfNulls(event.pointerCount)
                for (i in 0 until event.pointerCount) {
                    val oldCoords = MotionEvent.PointerCoords()
                    val pointerProperty = MotionEvent.PointerProperties()
                    event.getPointerCoords(i, oldCoords)
                    event.getPointerProperties(i, pointerProperty)
                    pointerCoords[i] = oldCoords
                    pointerCoords[i]!!.apply {
                        x = oldCoords.x
                        y = oldCoords.y
                    }
                    pointerProperties[i] = pointerProperty
                }

                val newEvent = MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    event.action,
                    event.pointerCount,
                    pointerProperties,
                    pointerCoords,
                    event.metaState,
                    event.buttonState,
                    event.xPrecision,
                    event.yPrecision,
                    event.deviceId,
                    event.edgeFlags,
                    event.source,
                    event.flags
                )
                val bytes = ParcelableUtil.marshall(newEvent)
                runBlocking {
                    eventChannel.send(byteArrayOf(0, *bytes))
                }
                newEvent.recycle()
                return@setOnTouchListener true
            }
        }
        baseBinding.btnClose.setOnClickListener {
            runIOCatching {
                client.post("http://${baseBinding.etIp.text}:${baseBinding.etPort.text}/close/$id")
                runMain {
                    baseBinding.tvId.text = "id: null"
                    id = null
                }
            }.onFailure {
                TipUtil.showTip(this@MainActivity, it)
            }
        }
    }

    var sendTime = 0L

    var targetId: String? = null

    val buffer = LinkedBlockingDeque<Triple<ByteArray, Int, Long>>()

    private fun getLocalUserName() = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
}