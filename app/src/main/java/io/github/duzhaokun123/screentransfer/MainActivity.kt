package io.github.duzhaokun123.screentransfer

import android.provider.Settings
import android.util.Log
import android.view.SurfaceHolder
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.screentransfer.databinding.ActivityMainBinding
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegDecoder
import io.github.duzhaokun123.screentransfer.netservice.NetService
import io.github.duzhaokun123.screentransfer.utils.AppendableByteInputStream
import io.github.duzhaokun123.screentransfer.xposed.IVideoStreamCallback
import io.github.duzhaokun123.screentransfer.xposed.ScrTsfManagerHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.util.concurrent.LinkedBlockingDeque


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::class.java, Config.NO_BACK) {
    var ffmpegDecoder = FFmpegDecoder(600, 800) {
        val canvas = baseBinding.surface.holder.lockHardwareCanvas()
        canvas.drawBitmap(it, 0F, 0F, null)
        baseBinding.surface.holder.unlockCanvasAndPost(canvas)
    }
    val appendableByteInputStream = AppendableByteInputStream()
    val netService = NetService()

    override fun initViews() {
        super.initViews()
        netService.ffmpegDecoder = ffmpegDecoder
        val client = HttpClient(CIO) {
            install(WebSockets) {
                pingInterval = 20_000
            }
        }
        var webSocketsSeason: DefaultClientWebSocketSession? = null
                runIO {
                  webSocketsSeason = client.webSocketSession(host = "127.0.0.1", port = 8043, path = "/video")
                    while (true) {
                        val f = webSocketsSeason?.incoming?.receive() as? Frame.Text ?: continue
                        TipUtil.showTip(this@MainActivity, f.readText())
                    }
        }
        baseBinding.btn1.setOnClickListener {

        }
        baseBinding.btn2.setOnClickListener {
            TipUtil.showTip(this, FFmpegDecoder.getVersion())
        }
        baseBinding.btn3.setOnClickListener {
            runIO {
                val sendTime = System.currentTimeMillis()
                TipUtil.showTip(this@MainActivity, client.get("http://192.168.3.216:8043/ping").bodyAsText() + " ${System.currentTimeMillis() - sendTime}")

            }
        }
        baseBinding.btn4.setOnClickListener {
            ScrTsfManagerHelper.createWindow(object : IVideoStreamCallback.Stub() {
                override fun onStream(bytes: ByteArray, flags: Int, timeUs: Long) {
                    runMain {
                        baseBinding.tvMessage.text = "${System.currentTimeMillis()} ${bytes.size}"
                    }
                    runIO {
                        webSocketsSeason?.send(bytes)
                    }
                }
            })
        }
        baseBinding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
//                netService.ffmpegDecoder = ffmpegDecoder
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }
        })
    }

    var sendTime = 0L

    var targetId: String? = null

    val buffer = LinkedBlockingDeque<Triple<ByteArray, Int, Long>>()

    private fun getLocalUserName() = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
}