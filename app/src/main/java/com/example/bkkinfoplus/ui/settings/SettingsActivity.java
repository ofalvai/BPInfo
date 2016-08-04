package com.example.bkkinfoplus.ui.settings;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.bkkinfoplus.BkkInfoApplication;
import com.example.bkkinfoplus.R;
import com.jakewharton.processphoenix.ProcessPhoenix;

import javax.inject.Inject;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsActivity";

    @Inject SharedPreferences mSharedPreferences;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);

        return intent;
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BkkInfoApplication.injector.inject(this);

        setupActionBar();

        addPreferencesFromResource(R.xml.pref_general);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_language)));

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Language
        if (key.equals(getString(R.string.pref_key_language))) {
            Preference languagePreference = findPreference(getString(R.string.pref_key_language));

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.pref_language_dialog_title))
                    .setMessage(getString(R.string.pref_language_dialog_message))
                    .setNegativeButton(getString(R.string.pref_language_dialog_negative_button), null)
                    .setPositiveButton(getString(R.string.pref_language_dialog_positive_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ProcessPhoenix.triggerRebirth(getApplicationContext());
                            }
                        })
                    .show();
        }
        // Debug mode
        else if (key.equals(getString(R.string.pref_key_debug_mode))) {
            boolean state = mSharedPreferences.getBoolean(key, false);
            String text = state ? getString(R.string.debug_mode_on) : getString(R.string.debog_mode_off);

            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_debug:
                final String key = getString(R.string.pref_key_debug_mode);
                boolean oldState = mSharedPreferences.getBoolean(key, false);
                mSharedPreferences.edit().putBoolean(key, !oldState).apply();
                break;
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
