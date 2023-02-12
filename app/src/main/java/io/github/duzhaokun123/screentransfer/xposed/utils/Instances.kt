package io.github.duzhaokun123.screentransfer.xposed.utils

import android.app.ActivityManager
import android.app.IActivityTaskManager
import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.IInputManager
import android.os.ServiceManager
import android.os.UserManager
import android.view.IWindowManager
import android.view.WindowManager
import com.android.internal.statusbar.IStatusBarService

object Instances {
    lateinit var windowManager: WindowManager
        private set
    lateinit var iWindowManager: IWindowManager
        private set
    lateinit var inputManager: IInputManager
        private set
    lateinit var displayManager: DisplayManager
        private set
    lateinit var activityTaskManager: IActivityTaskManager
        private set
    lateinit var packageManager: PackageManager
        private set
    lateinit var activityManager: ActivityManager
        private set
    lateinit var userManager: UserManager
        private set
    lateinit var iPackageManager: IPackageManager
        private set
    lateinit var iStatusBarService: IStatusBarService
        private set

    fun init(context: Context) {
        windowManager = context.getSystemService(WindowManager::class.java)
        iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"))
        inputManager = IInputManager.Stub.asInterface(ServiceManager.getService("input"))
        displayManager = context.getSystemService(DisplayManager::class.java)
        activityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"))
        packageManager = context.packageManager
        activityManager = context.getSystemService(ActivityManager::class.java)
        userManager = context.getSystemService(UserManager::class.java)
        iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        iStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))
    }
}