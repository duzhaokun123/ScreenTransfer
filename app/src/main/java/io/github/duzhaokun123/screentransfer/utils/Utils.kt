package io.github.duzhaokun123.screentransfer.utils

inline fun <T> Result<T>.onException(action: (exception: Exception) -> Unit): Result<T> =
    this.onFailure { t ->
        if (t is Error) throw t
        action(t as Exception)
    }