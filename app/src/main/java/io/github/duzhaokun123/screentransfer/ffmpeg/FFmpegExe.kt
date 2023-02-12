package io.github.duzhaokun123.screentransfer.ffmpeg

import android.app.Application
import android.os.Build
import java.io.File
import java.nio.charset.Charset

object FFmpegExe {
    init {
        System.loadLibrary("getlinker")
    }

    fun init(application: Application) {
        execFile = "${File(application.packageManager.getPackageInfo(application.packageName, 0).applicationInfo.sourceDir).path}!/lib/${Build.SUPPORTED_64_BIT_ABIS[0]}/libffmpeg.so"
//        tmpDir = application.cacheDir.path
//        home = application.filesDir.path
        linker = getLinker()
    }

    private lateinit var execFile: String
    private lateinit var tmpDir: String
    private lateinit var home: String
    private lateinit var linker: String

    fun commandLine(vararg arg: String) = Runtime.getRuntime().exec(arrayOf(linker, execFile, *arg))

    fun version() = commandLine("-version").let {
        it.waitFor()
        it.inputStream.readAllBytes().toString(Charset.forName("UTF-8"))
    }

    external fun getLinker(): String
}