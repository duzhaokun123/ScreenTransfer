package io.github.duzhaokun123.screentransfer.service

import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.service.display.RemoteDisplay
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class NetService {
    val streamCallbacks = mutableMapOf<String, StreamCallback>()

    val videoFrameCaches = mutableMapOf<String, Channel<ByteArray>>()

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
                    get("/newdisplay") {
                        val width = call.request.queryParameters["width"]?.toIntOrNull()
                        if (width == null) {
                            call.respond(HttpStatusCode.BadRequest, "miss width")
                            return@get
                        }
                        val height = call.request.queryParameters["height"]?.toIntOrNull()
                        if (height == null) {
                            call.respond(HttpStatusCode.BadRequest, "miss height")
                            return@get
                        }
                        val densityDpi = call.request.queryParameters["densityDpi"]?.toIntOrNull()
                        if (densityDpi == null) {
                            call.respond(HttpStatusCode.BadRequest, "miss densityDpi")
                            return@get
                        }
                        val id = onNewDisplay(width, height, densityDpi)
                        if (id != null) {
                            call.respond(id)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "no display available yet")
                        }
                    }
                    webSocket("/video/{id}") {
                        val id = call.parameters["id"]
                        val videoFrameCache = videoFrameCaches[id]
                        if (videoFrameCache == null) {
                            close(
                                CloseReason(
                                    CloseReason.Codes.CANNOT_ACCEPT,
                                    "no cache for $id"
                                )
                            )
                            return@webSocket
                        }
                        send("$id video ok")
                        while (true) {
                            send(videoFrameCache.receive())
                        }
                    }

                    webSocket("/event/{id}") {
                        val id = call.parameters["id"]
                        val streamCallback = streamCallbacks[id]
                        if (streamCallback == null) {
                            close(
                                CloseReason(
                                    CloseReason.Codes.CANNOT_ACCEPT,
                                    "no callback for $id"
                                )
                            )
                            return@webSocket
                        }
                        send("$id event ok")
                        for (frame in incoming) {
                            frame as? Frame.Binary ?: continue
                            streamCallback.onEvent(frame.readBytes())
                        }
                    }
//                    webSocket("/messager/{id}") {
//                        val id = call.parameters["id"]
//                        val streamCallback = streamCallbacks[id]
//                        if (streamCallback == null) {
//                            close(
//                                CloseReason(
//                                    CloseReason.Codes.CANNOT_ACCEPT,
//                                    "no callback for $id"
//                                )
//                            )
//                            return@webSocket
//                        }
//                        send("$id messager ok")
//                        streamCallbacks[id]?.onMessagerAvailable(this)
//                    }
                    post("/close/{id}") {
                        val id = call.parameters["id"]
                        val streamCallback = streamCallbacks[id]
                        if (streamCallback == null) {
                            call.respond(HttpStatusCode.NotFound, "no callback for $id")
                            return@post
                        }
                        streamCallback.onClose()
                        unregStreamCallback(id!!)
                        call.respond("$id ok")
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

    fun regStreamCallback(id: String, streamCallback: StreamCallback) {
        streamCallbacks[id] = streamCallback
    }

    fun unregStreamCallback(id: String) {
        streamCallbacks.remove(id)
    }

    fun onNewDisplay(width: Int, height: Int, densityDpi: Int): String? {
        try {
            val d = RemoteDisplay(width, height, densityDpi).streamCallback
            val id = d.getId().toString()
            regStreamCallback(id, d)
            val channel = Channel<ByteArray>()
            videoFrameCaches[id] = channel
            d.onVideoFrameSenderAvailable(object : ByteArraySender {
                override fun send(bytes: ByteArray) {
                    runBlocking {
                        channel.send(bytes)
                    }
                }
            })
            return id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    interface StreamCallback {
        companion object {
            const val EVENT_TYPE_MOTION: Byte = 0
        }

        fun onVideoFrameSenderAvailable(sender: ByteArraySender)
        fun onEvent(bytes: ByteArray)
        fun onClose()

        fun getId(): Int
    }

    interface ByteArraySender {
        fun send(bytes: ByteArray)
    }
}