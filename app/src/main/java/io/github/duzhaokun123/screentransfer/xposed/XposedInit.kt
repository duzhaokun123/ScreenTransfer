package io.github.duzhaokun123.screentransfer.xposed

import android.content.Context
import android.content.pm.IPackageManager
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findAllConstructors
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.screentransfer.BuildConfig
import io.github.duzhaokun123.screentransfer.xposed.utils.log

private const val TAG = "ScrTsf_XposedInit"

class XposedInit : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
         if (lpparam.packageName == "android") {
             log(TAG, "xposed init")
             log(TAG, "buildtype: ${BuildConfig.BUILD_TYPE}")
             EzXHelperInit.initHandleLoadPackage(lpparam)
             var serviceManagerHook: XC_MethodHook.Unhook? = null
             serviceManagerHook = findMethod("android.os.ServiceManager") {
                 name == "addService"
             }.hookBefore { param ->
                 if (param.args[0] == "package") {
                     serviceManagerHook?.unhook()
                     val pms = param.args[1] as IPackageManager
                     log(TAG, "Got pms: $pms")
                     runCatching {
                         BridgeService.register(pms)
                         log(TAG, "Bridge service injected")
                     }.onFailure {
                         log(TAG, "System service crashed", it)
                     }
                 }
             }
             var activityManagerServiceConstructorHook: List<XC_MethodHook.Unhook> = emptyList()
             activityManagerServiceConstructorHook = findAllConstructors("com.android.server.am.ActivityManagerService") {
                 parameterTypes[0] == Context::class.java
             }.hookAfter {
                 activityManagerServiceConstructorHook.forEach { hook -> hook.unhook() }
                 ScrTsfManager.systemContext = it.thisObject.getObjectAs("mUiContext")
                 log(TAG, "get systemUiContext")
             }.also {
                 if (it.isEmpty())
                     log(TAG, "no constructor with parameterTypes[0] == Context found")
             }
             var activityManagerServiceSystemReadyHook: XC_MethodHook.Unhook? = null
             activityManagerServiceSystemReadyHook = findMethod("com.android.server.am.ActivityManagerService") {
                 name == "systemReady"
             }.hookAfter {
                 activityManagerServiceSystemReadyHook?.unhook()
                 ScrTsfManager.systemReady()
                 log(TAG, "system ready")
             }
        }
    }
}