package io.github.duzhaokun123.screentransfer

import com.google.android.material.color.DynamicColors
import io.github.duzhaokun123.screentransfer.ffmpeg.FFmpegExe

lateinit var application: Application

class Application: android.app.Application() {
    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        FFmpegExe.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}