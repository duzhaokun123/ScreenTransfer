package io.github.duzhaokun123.screentransfer.service.xposed

import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import io.github.duzhaokun123.screentransfer.xposed.IScrTsfManager
import io.github.duzhaokun123.screentransfer.xposed.IStreamCallback
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ScrTsfManagerHelper : IScrTsfManager, IBinder.DeathRecipient {
    private const val TAG = "ScrTsfManagerHelper"

    private class ServiceProxy(private val obj: IScrTsfManager) : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            val result = method.invoke(obj, *args.orEmpty())
            if (result == null) Log.i(TAG, "Call service method ${method.name}")
            else Log.i(TAG, "Call service method ${method.name} with result " + result.toString().take(20))
            return result
        }
    }

    @Volatile
    private var service: IScrTsfManager? = null

    override fun binderDied() {
        service = null
        Log.e(TAG, "Binder died")
    }

    override fun asBinder() = service?.asBinder()

    override fun getVersionName(): String? {
        return getService()?.versionName
    }

    override fun getVersionCode(): Int {
        return getService()?.versionCode ?: 0
    }

    override fun getBuildTime(): Long {
        return getService()?.buildTime ?: 0
    }

    override fun createDisplay(width: Int, height: Int, densityDpi: Int): IStreamCallback? {
        return getService()?.createDisplay(width, height, densityDpi)
    }

    private fun getService(): IScrTsfManager? {
        if (service != null) return service
        val pm = ServiceManager.getService("package")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        val remote = try {
            data.writeInterfaceToken(BridgeService.DESCRIPTOR)
            data.writeInt(BridgeService.ACTION_GET_BINDER)
            pm.transact(BridgeService.TRANSACTION, data, reply, 0)
            reply.readException()
            val binder = reply.readStrongBinder()
            IScrTsfManager.Stub.asInterface(binder)
        } catch (e: RemoteException) {
            Log.d(TAG, "Failed to get binder")
            null
        } finally {
            data.recycle()
            reply.recycle()
        }
        if (remote != null) {
            Log.i(TAG, "Binder acquired")
            remote.asBinder().linkToDeath(this, 0)
            service = Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(IScrTsfManager::class.java),
                ServiceProxy(remote)
            ) as IScrTsfManager
        }
        return service
    }
}