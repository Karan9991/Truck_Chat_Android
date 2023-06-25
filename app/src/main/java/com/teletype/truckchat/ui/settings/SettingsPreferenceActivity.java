package com.teletype.truckchat.ui.settings;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.teletype.truckchat.R;
import com.teletype.truckchat.ui.settings.SettingsAboutFragment;
import com.teletype.truckchat.ui.settings.SettingsMessagesPreferenceFragment;
import com.teletype.truckchat.ui.settings.SettingsNotificationsPreferenceFragment;

import java.util.List;


public class SettingsPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.app_name_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers_settings, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsAboutFragment.class.getName().equals(fragmentName) ||
                SettingsMessagesPreferenceFragment.class.getName().equals(fragmentName) ||
                SettingsNotificationsPreferenceFragment.class.getName().equals(fragmentName);
    }
}
