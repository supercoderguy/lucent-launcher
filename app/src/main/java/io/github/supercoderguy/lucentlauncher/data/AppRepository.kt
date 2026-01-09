package io.github.supercoderguy.lucentlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import io.github.supercoderguy.lucentlauncher.model.DockItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun getInstalledApps(): Flow<List<DockItem.App>> = flow {
        val user = Process.myUserHandle()
        val apps = launcherApps.getActivityList(null, user).map {
            DockItem.App(
                label = it.label.toString(),
                packageName = it.applicationInfo.packageName,
                componentName = it.name
            )
        }.sortedBy { it.label.lowercase() }
        emit(apps)
    }

    fun launchApp(app: DockItem.App) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    }
}
