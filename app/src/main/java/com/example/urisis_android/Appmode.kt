package com.example.urisis_android.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Runtime mode flag — controls whether the app behaves as a lab training
 * device or as an end-user production install.
 *
 * Stored per-device in SharedPreferences. Defaults to PRODUCTION so that
 * a freshly installed APK from the Play Store never accidentally lands in
 * Training Mode.
 *
 * Toggle via the hidden 7-tap gesture on the About screen's version label.
 */
enum class AppMode {
    /** Normal Urino experience: load frozen model, classify samples. */
    PRODUCTION,

    /**
     * Lab-only experience: collect labeled measurements, run grid search,
     * export trained_model.json. The production UI is still accessible so
     * you can sanity-check classifications during training.
     */
    TRAINING;
}

/**
 * Persists and broadcasts the current [AppMode]. Singleton — inject via
 * Hilt or instantiate once in [UrinoApplication].
 */
class AppModeRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMode(): AppMode {
        val name = prefs.getString(KEY_MODE, AppMode.PRODUCTION.name)
        return runCatching { AppMode.valueOf(name!!) }.getOrDefault(AppMode.PRODUCTION)
    }

    fun setMode(mode: AppMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    fun isTraining(): Boolean = getMode() == AppMode.TRAINING

    companion object {
        private const val PREFS_NAME = "urino_app_mode"
        private const val KEY_MODE = "current_mode"
    }
}