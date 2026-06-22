package com.timetopay

import android.content.Context

object TargetPackages {
    private const val PREFS_NAME = "timetopay_prefs"
    private const val KEY_SELECTED = "selected_packages"

    fun getSelected(context: Context): Set<String> {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED, emptySet())
        return stored?.toSet() ?: emptySet()
    }

    fun setSelected(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED, HashSet(packages))
            .apply()
    }

    fun hasSelection(context: Context): Boolean = getSelected(context).isNotEmpty()
}
