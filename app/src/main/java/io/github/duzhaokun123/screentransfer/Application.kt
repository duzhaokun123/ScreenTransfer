package io.github.duzhaokun123.screentransfer

import com.google.android.material.color.DynamicColors
import io.github.duzhaokun123.screentransfer.service.NetService

lateinit var application: Application
lateinit var netService: NetService

class Application: android.app.Application() {
    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        netService = NetService()
    }
}