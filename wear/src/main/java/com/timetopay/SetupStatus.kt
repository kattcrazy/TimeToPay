package com.timetopay

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.provider.Settings

object SetupStatus {
    fun hasSecureSettingsPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, TimeToPayAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        return enabledServices
            .split(':')
            .map { it.trim() }
            .any { it.equals(expected, ignoreCase = true) }
    }
}
