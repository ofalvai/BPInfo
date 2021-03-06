/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.util

import android.content.Context
import android.content.SharedPreferences
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

    private const val LANG_CODE_AUTO = "auto"
    private const val LANG_CODE_ENGLISH = "en"
    private const val LANG_CODE_HUNGARIAN = "hu"

    private const val PREF_KEY_LANGUAGE = "language" // TODO: remove the same key from strings.xml after refactor

    fun setLocale(c: Context): Context {
        return updateResources(c)
    }

    /**
     * Gets the current language's language code for API calls.
     * If a language has been set in the preferences, it reads the value from SharedPreferences.
     * If it has been set to "auto" or unset, it decides based on the current locale, using "en" for
     * any other language than Hungarian ("hu")
     * @return The app's current language's code.
     */
    fun getCurrentLanguageCode(sharedPreferences: SharedPreferences): String {
        var languageCode = sharedPreferences.getString(
                PREF_KEY_LANGUAGE,
                LANG_CODE_AUTO
        )!!

        if (languageCode == LANG_CODE_AUTO) {
            languageCode = if (Locale.getDefault().language == LANG_CODE_HUNGARIAN) {
                LANG_CODE_HUNGARIAN
            } else {
                LANG_CODE_ENGLISH
            }
        }

        return languageCode
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
        )!!
    }
}