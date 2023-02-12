package io.github.duzhaokun123.screentransfer.utils

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.LinkedBlockingDeque

class AppendableByteInputStream: InputStream() {
    val q = LinkedBlockingDeque<ByteArrayInputStream>()

    var last: ByteArrayInputStream? = null

    fun append(b: ByteArrayInputStream) {
        q.put(b)
        Log.d("AppendableByteInputStream", "append: ${q.size}")
    }

    @Synchronized
    override fun read(): Int {
        val r = last?.read()
        if (r == null || r == -1) {
            last = q.take()
            return this.read()
        }
        return r
    }
}