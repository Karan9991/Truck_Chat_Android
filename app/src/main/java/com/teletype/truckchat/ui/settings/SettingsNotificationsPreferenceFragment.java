package com.teletype.truckchat.ui.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.text.TextUtils;

import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;


public class SettingsNotificationsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_settings_notifications);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sharedPreferences = Utils.getSharedPreferences(getActivity());
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_NOTIFICATION);
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_CHAT_TONE);
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_NOTIFICATION_RINGTONE);
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_NOTIFICATION_VIBRATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);

        if (Constants.PREFS_CHAT_TONE.equals(key)) {
            pref.setSummary((String) getSummary(key, sharedPreferences.getBoolean(key, true)));
        } else if (Constants.PREFS_NOTIFICATION.equals(key)) {
            pref.setSummary((String) getSummary(key, sharedPreferences.getBoolean(key, true)));
        } else if (Constants.PREFS_NOTIFICATION_RINGTONE.equals(key)) {
            pref.setSummary((String) getSummary(key, sharedPreferences.getString(key, Settings.System.DEFAULT_NOTIFICATION_URI.toString())));
        } else if (Constants.PREFS_NOTIFICATION_VIBRATE.equals(key)) {
            pref.setSummary((String) getSummary(key, sharedPreferences.getBoolean(key, true)));
        }
    }

    private Object getSummary(String key, boolean value) {
        if (Constants.PREFS_CHAT_TONE.equals(key)) {
            return getString(value ? R.string.pref_summary_chat_tone_on : R.string.pref_summary_chat_tone_off);
        } else if (Constants.PREFS_NOTIFICATION.equals(key)) {
            if (value) {
                findPreference(Constants.PREFS_NOTIFICATION_RINGTONE).setEnabled(true);
                findPreference(Constants.PREFS_NOTIFICATION_VIBRATE).setEnabled(true);
            } else {
                findPreference(Constants.PREFS_NOTIFICATION_RINGTONE).setEnabled(false);
                findPreference(Constants.PREFS_NOTIFICATION_VIBRATE).setEnabled(false);
            }
            return getString(value ? R.string.pref_summary_notification_on : R.string.pref_summary_notification_off);
        } else if (Constants.PREFS_NOTIFICATION_VIBRATE.equals(key)) {
            return getString(value ? R.string.pref_summary_notification_vibrate_on : R.string.pref_summary_notification_vibrate_off);
        }

        return null;
    }

    private Object getSummary(String key, String value) {
        if (Constants.PREFS_NOTIFICATION_RINGTONE.equals(key)) {
            if (TextUtils.isEmpty(value)) {
                return getString(R.string.pref_summary_notification_ringtone_off);
            } else {
                Uri ringtone = Uri.parse(value);

                if (Settings.System.DEFAULT_ALARM_ALERT_URI.equals(ringtone)) {
                    return getString(R.string.pref_summary_notification_ringtone_default_alarm);
                } else if (Settings.System.DEFAULT_NOTIFICATION_URI.equals(ringtone)) {
                    return getString(R.string.pref_summary_notification_ringtone_default_notification);
                } else if (Settings.System.DEFAULT_RINGTONE_URI.equals(ringtone)) {
                    return getString(R.string.pref_summary_notification_ringtone_default_ringtone);
                } else {
                    Activity activity = getActivity();
                    return getString(R.string.pref_summary_notification_ringtone_on, RingtoneManager.getRingtone(activity, ringtone).getTitle(activity));
                }
            }
        }

        return null;
    }
}
