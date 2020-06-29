package org.nano.updater.util

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefUtils @Inject constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)

    fun toggleDisplayLogs(boolean: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_DISPLAY_LOGS_TOGGLE, boolean).apply()
    }

    fun toggleRebootAfterFlash(boolean: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_REBOOT_AFTER_FLASH, boolean).apply()
    }

    fun toggleCheckUpdatesPeriodically(boolean: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_CHECK_UPDATES_PERIODICALLY, boolean).apply()
    }

    fun toggleCleanupDir(boolean: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_CLEANUP_DIR, boolean).apply()
    }

    fun isDisplayLogsToggled() = sharedPreferences.getBoolean(Constants.KEY_DISPLAY_LOGS_TOGGLE, false)

    fun isRebootAfterFlashToggled() = sharedPreferences.getBoolean(Constants.KEY_REBOOT_AFTER_FLASH, false)

    fun isCheckUpdatesPeriodicallyToggled() = sharedPreferences.getBoolean(Constants.KEY_CHECK_UPDATES_PERIODICALLY, false)

    fun isCleanupDirToggled() = sharedPreferences.getBoolean(Constants.KEY_CLEANUP_DIR, false)
}