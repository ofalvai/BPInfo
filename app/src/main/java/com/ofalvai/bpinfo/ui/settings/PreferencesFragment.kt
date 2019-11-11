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

package com.ofalvai.bpinfo.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jakewharton.processphoenix.ProcessPhoenix
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity
import com.ofalvai.bpinfo.util.Analytics
import org.koin.android.ext.android.inject

class PreferencesFragment : PreferenceFragmentCompat() {

    private val analytics: Analytics by inject()
    private val sharedPreferences: SharedPreferences by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(shouldShowDebugMode())
        activity?.setTitle(R.string.title_activity_settings)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_debug -> {
                val key = getString(R.string.pref_key_debug_mode)
                val oldState = sharedPreferences.getBoolean(key, false)
                sharedPreferences.edit().putBoolean(key, !oldState).apply()
            }
        }

        return true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        initLanguagePrefs()
        initDataSourcePrefs()
        initNotificationPrefs()
    }

    private fun initDataSourcePrefs() {
        findPreference<ListPreference>(getString(R.string.pref_key_data_source))?.setOnPreferenceChangeListener { _, _ ->
            Toast.makeText(requireContext(), R.string.data_source_changed_refreshed, Toast.LENGTH_SHORT).show()
            analytics.logDataSourceChange()

            // Recreating AlertListActivity. This relies on BpInfoApplication's preference listener,
            // which can rebuild the Dagger dependency graph so that the new Activity (and its
            // Fragments' presenters) will use the new data source
            val intent = Intent(requireContext(), AlertListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this.startActivity(intent)

            true
        }
    }

    private fun initLanguagePrefs() {
        findPreference<ListPreference>(getString(R.string.pref_key_language))?.setOnPreferenceChangeListener { _, newValue ->
            analytics.logLanguageChange(newValue.toString())
            showLanguageRestartDialog()
            true
        }
    }

    private fun showLanguageRestartDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.pref_language_dialog_title))
                .setMessage(getString(R.string.pref_language_dialog_message))
                .setNegativeButton(getString(R.string.pref_language_dialog_negative_button), null)
                .setPositiveButton(getString(R.string.pref_language_dialog_positive_button)) { _, _ ->
                    ProcessPhoenix.triggerRebirth(activity?.applicationContext)
                }
                .show()
    }

    /**
     * Only show debug mode when development settings are enabled on the device
     */
    private fun shouldShowDebugMode(): Boolean {
        var devOptions = 0
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            @Suppress("DEPRECATION")
            devOptions = Settings.Secure.getInt(
                    requireActivity().contentResolver,
                    Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED,
                    0
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            devOptions = Settings.Secure.getInt(
                    requireActivity().contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0
            )
        }

        return devOptions == 1
    }

    private fun initNotificationPrefs() {
        val pref = findPreference<Preference>(getString(R.string.pref_key_notifications))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // The system notification preference screen is unavailable on pre-Lollipop
            pref?.isVisible = false
        } else {
            pref?.setOnPreferenceClickListener {
                launchSystemNotificationPrefs()
                analytics.logNotificationChannelsOpened()
                true
            }
        }

        findPreference<Preference>(getString(R.string.pref_key_notifications_routes))
                ?.setOnPreferenceClickListener {
                    analytics.logNotificationFromSettingsOpened()
                    false // Launch intent is defined in XML
                }
    }

    private fun launchSystemNotificationPrefs() {
        val intent = Intent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, getString(R.string.notif_channel_alerts_id))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", requireActivity().packageName)
            intent.putExtra("app_uid", requireActivity().applicationInfo.uid)
        }

        startActivity(intent)
    }
}