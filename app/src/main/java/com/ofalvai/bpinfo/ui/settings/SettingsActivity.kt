/*
 * Copyright 2016 Olivér Falvai
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ofalvai.bpinfo.ui.settings


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.jakewharton.processphoenix.ProcessPhoenix
import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity
import com.ofalvai.bpinfo.util.FabricUtils
import javax.inject.Inject

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
 * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
 * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit internal var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BpInfoApplication.injector.inject(this)

        setupActionBar()

        addPreferencesFromResource(R.xml.pref_general)
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_language)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_data_source)))

    }

    override fun onResume() {
        super.onResume()
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.pref_key_language) -> {
                val languageValue = sharedPreferences.getString(key, "default")
                FabricUtils.logLanguageChange(languageValue)
                showLanguageRestartDialog()
            }
            getString(R.string.pref_key_debug_mode) -> {
                val state = mSharedPreferences.getBoolean(key, false)
                val text = if (state) getString(R.string.debug_mode_on) else getString(R.string.debog_mode_off)
                Toast.makeText(this, text, Toast.LENGTH_LONG).show()
                FabricUtils.logDebugMode(state.toString())
            }
            getString(R.string.pref_key_data_source) -> {
                Toast.makeText(this, R.string.data_source_changed_refreshed, Toast.LENGTH_SHORT).show()
                FabricUtils.logDataSourceChange(mSharedPreferences.getString(key, ""))

                // Recreating AlertListActivity. This relies on BpInfoApplication's preference listener,
                // which can rebuild the Dagger dependency graph so that the new Activity (and its
                // Fragments' presenters) will use the new data source
                val intent = Intent(this, AlertListActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this.startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (shouldShowDebugMode()) {
            menuInflater.inflate(R.menu.menu_settings, menu)
            return true
        } else {
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_debug -> {
                val key = getString(R.string.pref_key_debug_mode)
                val oldState = mSharedPreferences.getBoolean(key, false)
                mSharedPreferences.edit().putBoolean(key, !oldState).apply()
            }
        }

        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showLanguageRestartDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.pref_language_dialog_title))
                .setMessage(getString(R.string.pref_language_dialog_message))
                .setNegativeButton(getString(R.string.pref_language_dialog_negative_button), null)
                .setPositiveButton(getString(R.string.pref_language_dialog_positive_button)) { _, _ ->
                    ProcessPhoenix.triggerRebirth(applicationContext)
                }
                .show()
    }

    /**
     * Only show debug mode when development settings are enabled on the device
     */
    private fun shouldShowDebugMode(): Boolean {
        var devOptions = 0
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            devOptions = Settings.Secure.getInt(this.contentResolver, Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            devOptions = Settings.Secure.getInt(this.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        }

        return devOptions == 1
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
