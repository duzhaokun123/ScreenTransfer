package io.github.duzhaokun123.screentransfer.service.xposed

import android.annotation.SuppressLint
import android.content.Context
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.display.RemoteDisplay
import io.github.duzhaokun123.screentransfer.service.NetService
import io.github.duzhaokun123.screentransfer.service.xposed.utils.Instances
import io.github.duzhaokun123.screentransfer.service.xposed.utils.TipUtil
import io.github.duzhaokun123.screentransfer.service.xposed.utils.log
import io.github.duzhaokun123.screentransfer.xposed.IScrTsfManager
import io.github.duzhaokun123.screentransfer.xposed.IStreamCallback

class ScrTsfManager : IScrTsfManager.Stub() {
    companion object {
        const val TAG = "ScrTsfManager"
        @SuppressLint("StaticFieldLeak")
        var instance: ScrTsfManager? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var systemContext: Context

        fun systemReady() {
            TipUtil.init(systemContext, "[ScrTsf] ")
            Instances.init(systemContext)
        }
    }

    init {
        instance = this
        log(TAG, "ScrTsf service initialized")
    }

    override fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun getBuildTime(): Long {
        return BuildConfig.BUILD_TIME
    }

    override fun createDisplay(width: Int, height: Int, densityDpi: Int): IStreamCallback? {
        var r: IStreamCallback? = null
        runMain {
            val d = RemoteDisplay(width, height, densityDpi)
            r = d.streamCallback
        }
        while (r == null) {
            Thread.yield()
        }
        return r
    }
}