package com.timetopay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object AppLauncher {
    private const val TAG = "TimeToPay"

    fun restartApp(context: Context, packageName: String): Boolean {
        val launchIntent = resolveLaunchIntent(context, packageName) ?: return false
        return try {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            context.startActivity(launchIntent)
            Log.i(TAG, "Restarted $packageName")
            true
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to restart $packageName", exception)
            false
        }
    }

    fun resolveLaunchIntent(context: Context, packageName: String): Intent? {
        val packageManager = context.packageManager
        packageManager.getLaunchIntentForPackage(packageName)?.let { return it }

        val mainIntent = Intent(Intent.ACTION_MAIN).setPackage(packageName)
        val resolved = packageManager.resolveActivity(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null

        return Intent(Intent.ACTION_MAIN).apply {
            setClassName(
                resolved.activityInfo.packageName,
                resolved.activityInfo.name,
            )
        }
    }
}
