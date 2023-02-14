package io.github.duzhaokun123.screentransfer.service.xposed.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import io.github.duzhaokun123.androidapptemplate.utils.runMain

@SuppressLint("StaticFieldLeak")
object TipUtil {
    private lateinit var context: Context
    private lateinit var prefix: String

    fun init(context: Context, prefix: String = "") {
        TipUtil.context = context
        TipUtil.prefix = prefix
    }


    fun showToast(msg: String) {
        runMain {
            Toast.makeText(context, "$prefix$msg", Toast.LENGTH_LONG).show()
        }
    }
}