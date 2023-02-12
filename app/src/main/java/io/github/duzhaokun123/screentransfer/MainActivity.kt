package io.github.duzhaokun123.screentransfer

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.provider.Settings
import android.util.Log
import android.view.SurfaceHolder
import androidx.collection.SimpleArrayMap
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.androidapptemplate.utils.runNewThread
import io.github.duzhaokun123.screentransfer.databinding.ActivityMainBinding
import io.github.duzhaokun123.screentransfer.display.RemoteDisplay
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegExe
import io.github.duzhaokun123.screentransfer.utils.AppendableByteInputStream
import io.github.duzhaokun123.screentransfer.utils.FFmpegDecoder
import io.github.duzhaokun123.screentransfer.xposed.IVideoStreamCallback
import io.github.duzhaokun123.screentransfer.xposed.ScrTsfManagerHelper
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.LinkedBlockingDeque


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::class.java, Config.NO_BACK) {
    lateinit var ffmpegDecoder: FFmpegDecoder
    val appendableByteInputStream = AppendableByteInputStream()

    override fun initViews() {
        super.initViews()

        baseBinding.btn1.setOnClickListener {
            startAdvertising()
        }
        baseBinding.btn2.setOnClickListener {
            startDiscovery()
        }
        baseBinding.btn3.setOnClickListener {
            if (targetId == null) {
                TipUtil.showTip(this, "not connected")
            } else {
                sendTime = System.currentTimeMillis()
                Nearby.getConnectionsClient(this).sendPayload(targetId!!, Payload.fromBytes("ping".toByteArray()))
            }
        }
        baseBinding.btn4.setOnClickListener {
            Nearby.getConnectionsClient(this).sendPayload(targetId!!, Payload.fromStream(appendableByteInputStream))
            ScrTsfManagerHelper.createWindow(object : IVideoStreamCallback.Stub() {
                override fun onStream(bytes: ByteArray, flags: Int, timeUs: Long) {
                    Log.d("ScrTsf_RD", "onStream: ${bytes.size}")
//                    ffmpegDecoder.write(bytes)
                    appendableByteInputStream.append(ByteArrayInputStream(bytes))
//                    Nearby.getConnectionsClient(this@MainActivity).sendPayload(targetId!!, Payload.fromStream(ByteArrayInputStream(bytes)))
                }
            })
        }
        baseBinding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                ffmpegDecoder = FFmpegDecoder(baseBinding.surface.holder, 600, 800)
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

    val payloadCallback = object : PayloadCallback() {
        val backgroundThreads = SimpleArrayMap<Long, Thread>()

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                runMain {
                    baseBinding.tvMessage.text = payload.asBytes()!!.toString(Charset.defaultCharset())
                }
            }
            if (payload.type == Payload.Type.STREAM) {
                val backgroundThread = runNewThread {
                    val input = payload.asStream()!!.asInputStream()
                    while (Thread.interrupted().not()) {
                        try {
                            val availableBytes = input.available()
                            if (availableBytes > 0) {
                                val bytes = ByteArray(availableBytes)
                                if (input.read(bytes) == availableBytes) {
                                    runMain {
                                        baseBinding.tvMessage.text = "${System.currentTimeMillis()} $availableBytes"
                                    }
                                    Log.d("ScrTsf_RD", "onPayloadReceived: ${bytes.size}")
//                                    buffer.offer(Tr)
                                    ffmpegDecoder.write(bytes)
                                }
                            }
                        } catch (e: IOException) {
                            TipUtil.showTip(this@MainActivity, e)
                            break
                        }
                    }
                }
                backgroundThreads.put(payload.id, backgroundThread)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (backgroundThreads.containsKey(update.payloadId) && update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                backgroundThreads[update.payloadId]?.interrupt()
            }
        }
    }

    val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            TipUtil.showTip(this@MainActivity, "init ${connectionInfo.endpointName}")
            Nearby.getConnectionsClient(this@MainActivity).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            targetId = endpointId
        }

        override fun onDisconnected(endpointId: String) {

        }
    }

    private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(this)
            .startAdvertising(
                getLocalUserName(),
                BuildConfig.APPLICATION_ID, connectionLifecycleCallback, advertisingOptions
            ).addOnSuccessListener {
                TipUtil.showTip(this, "success")
            }.addOnFailureListener { e ->
                TipUtil.showTip(this@MainActivity, e)
            }
    }

    val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(info.endpointName)
                .setMessage(info.serviceId)
                .setPositiveButton("connect") { _, _ ->
                    Nearby.getConnectionsClient(this@MainActivity)
                        .requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
                        .addOnSuccessListener {
                            TipUtil.showTip(this@MainActivity, "success")
                        }.addOnFailureListener { e ->
                            TipUtil.showTip(this@MainActivity, e)
                        }
                }
                .show()
        }

        override fun onEndpointLost(endpointId: String) {

        }

    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(this)
            .startDiscovery(BuildConfig.APPLICATION_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                TipUtil.showTip(this, "success")
            }
            .addOnFailureListener { e ->
                TipUtil.showTip(this, e)
            }
    }

    private fun getLocalUserName() = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
}