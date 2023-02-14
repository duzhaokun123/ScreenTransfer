package io.github.duzhaokun123.screentransfer.service.xposed

import android.annotation.SuppressLint
import android.content.Context
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.service.NetService
import io.github.duzhaokun123.screentransfer.service.xposed.utils.Instances
import io.github.duzhaokun123.screentransfer.service.xposed.utils.TipUtil
import io.github.duzhaokun123.screentransfer.service.xposed.utils.log
import io.github.duzhaokun123.screentransfer.xposed.IScrTsfManager

class ScrTsfManager : IScrTsfManager.Stub() {
    companion object {
        const val TAG = "ScrTsfManager"
        @SuppressLint("StaticFieldLeak")
        var instance: ScrTsfManager? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var systemContext: Context
        lateinit var netService: NetService

        fun systemReady() {
            TipUtil.init(systemContext, "[ScrTsf] ")
            Instances.init(systemContext)
            netService = NetService()
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
}