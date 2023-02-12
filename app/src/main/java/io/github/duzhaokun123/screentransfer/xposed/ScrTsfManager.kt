package io.github.duzhaokun123.screentransfer.xposed

import android.annotation.SuppressLint
import android.content.Context
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.display.RemoteDisplay
import io.github.duzhaokun123.screentransfer.xposed.utils.Instances
import io.github.duzhaokun123.screentransfer.xposed.utils.TipUtil
import io.github.duzhaokun123.screentransfer.xposed.utils.log

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

    override fun createWindow(iVideoStreamCallback: IVideoStreamCallback) {
        runMain {
            RemoteDisplay(600, 800, 200, iVideoStreamCallback)
        }
    }

    override fun getBuildTime(): Long {
        return BuildConfig.BUILD_TIME
    }
}