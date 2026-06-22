package kattcrazy.timetopay

import android.content.Context
import android.content.SharedPreferences

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
            .commit()
    }

    fun hasSelection(context: Context): Boolean = getSelected(context).isNotEmpty()

    fun registerOnSelectionChanged(
        context: Context,
        onChanged: () -> Unit,
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SELECTED) {
                onChanged()
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun unregisterOnSelectionChanged(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }
}
