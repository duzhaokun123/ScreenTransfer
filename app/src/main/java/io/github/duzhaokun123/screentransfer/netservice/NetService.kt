package io.github.duzhaokun123.screentransfer.netservice

import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegDecoder
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*

class NetService {
    var ffmpegDecoder: FFmpegDecoder? = null

    init {
        runNewThread {
            embeddedServer(CIO, port = 8043) {
                install(WebSockets)
                routing {
                    get("/version") {
                        call.respondText("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    }
                    get("/ping") {
                        call.respondText("pong")
                    }
                    post("/video") {
                        ffmpegDecoder?.write(call.receive())
                        call.respond("ok")
                    }
                    webSocket("/video") {
                        send("ok")
                        for (frame in incoming) {
                            frame as? Frame.Binary ?: continue
                            ffmpegDecoder?.write(frame.data)
                        }
                    }
                    webSocket("/echo") {
                        send("You are connected!")
                        for (frame in incoming) {
                            frame as? Frame.Text ?: continue
                            val t = frame.readText()
                            send(t)
                        }
                    }
                }
            }.start(wait = true)
        }
    }
}