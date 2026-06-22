package com.timetopay

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

object LaunchableApps {
    fun query(context: android.content.Context): List<LaunchableApp> {
        val packageManager = context.packageManager

        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.enabled }
            .filter { it.packageName != context.packageName }
            .filter { matchesPaymentKeyword(packageManager, it) }
            .filter { isUsablePaymentApp(packageManager, it) }
            .mapNotNull { appInfo -> toLaunchableApp(packageManager, appInfo) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun isUsablePaymentApp(
        packageManager: PackageManager,
        appInfo: ApplicationInfo,
    ): Boolean {
        if (hasLaunchableActivity(packageManager, appInfo.packageName)) {
            return true
        }

        // Wear apps like Google Wallet may not expose a launcher intent to third-party apps,
        // but accessibility can still detect them in the foreground by package name.
        return packageNameLooksLikePaymentApp(appInfo.packageName)
    }

    private fun hasLaunchableActivity(
        packageManager: PackageManager,
        packageName: String,
    ): Boolean {
        if (packageManager.getLaunchIntentForPackage(packageName) != null) {
            return true
        }

        val mainIntent = Intent(Intent.ACTION_MAIN).setPackage(packageName)
        if (packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL).isNotEmpty()) {
            return true
        }

        return packageManager.resolveActivity(mainIntent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    private fun matchesPaymentKeyword(
        packageManager: PackageManager,
        appInfo: ApplicationInfo,
    ): Boolean {
        val label = appInfo.loadLabel(packageManager).toString()
        return containsPaymentKeyword(label) || containsPaymentKeyword(appInfo.packageName)
    }

    private fun packageNameLooksLikePaymentApp(packageName: String): Boolean {
        return containsPaymentKeyword(packageName)
    }

    private fun containsPaymentKeyword(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("pay") || lower.contains("wallet")
    }

    private fun toLaunchableApp(
        packageManager: PackageManager,
        appInfo: ApplicationInfo,
    ): LaunchableApp? {
        return try {
            LaunchableApp(
                packageName = appInfo.packageName,
                label = appInfo.loadLabel(packageManager).toString(),
                icon = appInfo.loadIcon(packageManager),
            )
        } catch (_: Exception) {
            null
        }
    }
}
