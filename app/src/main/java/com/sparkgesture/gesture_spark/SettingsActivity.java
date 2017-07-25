package com.sparkgesture.gesture_spark;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        LayoutInflater inflater = LayoutInflater.from(this);
        final SharedPreferences shareprefs = getPreferenceManager().getSharedPreferences();

        Resources resources = getResources();

        List<ListPreference> prefs = new ArrayList<>();

        final ListPreference gestureActionSettings0 = (ListPreference) findPreference("gesture0_action_settings");
        prefs.add(gestureActionSettings0);
        final ListPreference gestureActionSettings1 = (ListPreference) findPreference("gesture1_action_settings");
        prefs.add(gestureActionSettings1);
        final ListPreference gestureActionSettings2 = (ListPreference) findPreference("gesture2_action_settings");
        prefs.add(gestureActionSettings2);
        final ListPreference gestureActionSettings3 = (ListPreference) findPreference("gesture3_action_settings");
        prefs.add(gestureActionSettings3);

        for (final ListPreference lp : prefs) {
            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    int index = lp.findIndexOfValue(newValue.toString());
                    if (index != -1)
                    {
                        SharedPreferences.Editor sharedPrefsEditor = shareprefs.edit();
                        sharedPrefsEditor.putString("action", lp.getEntries()[index].toString());
                        sharedPrefsEditor.apply();
                    }
                    return true;
                }
            });
        }




        Preference button = findPreference("save_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return true;
            }
        });
    }
}
