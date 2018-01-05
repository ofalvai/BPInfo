package com.ofalvai.bpinfo.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.preference.PreferenceManager
import com.ofalvai.bpinfo.R
import java.util.*

/**
 * Helper class for changing the locale of Resources based on user preference.
 * Based on https://github.com/YarikSOffice/LanguageTest/blob/master/app/src/main/java/com/yariksoffice/languagetest/LocaleManager.java
 */
object LocaleManager {

    fun setLocale(c: Context): Context {
        return updateResources(c)
    }

    private fun updateResources(context: Context): Context {
        val selectedLanguage = getSelectedLanguage(context)
        if (selectedLanguage == context.getString(R.string.pref_key_language_auto)) {
            // Language is "auto". This is either because the preference is missing,
            // or because it has been set to "auto"
            return context
        }

        var newContext = context
        val locale = Locale(selectedLanguage)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale)
            newContext = context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
        return newContext
    }

    /**
     * Returns the selected language from Shared Preferences, or the default R.string.pref_key_language_auto
     */
    private fun getSelectedLanguage(c: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(c)
        return prefs.getString(
                c.getString(R.string.pref_key_language),
                c.getString(R.string.pref_key_language_auto)
        )
    }
}