package com.teletype.truckchat.ui.settings;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;


public class SettingsMessagesPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_settings_messages);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sharedPreferences = Utils.getSharedPreferences(getActivity());
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_MSG_HANDLE);
        onSharedPreferenceChanged(sharedPreferences, Constants.PREFS_AVATAR);
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

        if (key.equals(Constants.PREFS_MSG_HANDLE)) {
            pref.setSummary((String) getSummary(key, sharedPreferences.getString(key, "")));
        }
        if (key.equals(Constants.PREFS_AVATAR)) {
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(DialogAvatarPreference.IS_CLOSE_VISIBLE, true);
                    FragmentManager manager = getFragmentManager();
                    DialogAvatarPreference dialog = new DialogAvatarPreference();
                    dialog.setArguments(bundle);
                    dialog.show(manager, dialog.getClass().getName());
                    return false;
                }
            });
        }
    }

    private Object getSummary(String key, String value) {
        if (key.equals(Constants.PREFS_MSG_HANDLE)) {
            if (TextUtils.isEmpty(value)) {
                return getString(R.string.pref_summary_handle_default);
            } else {
                return getString(R.string.pref_summary_handle, getString(R.string.pref_format_handle, value));
            }
        }

        return null;
    }
}
