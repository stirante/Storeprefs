package com.stirante.storeprefs.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.stirante.storeprefs.R;
import com.stirante.storeprefs.utils.SimpleDatabase;

import java.util.HashMap;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fragment_preference);
            getPreferenceManager().findPreference("reset_ignore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SimpleDatabase.load();
                    SimpleDatabase.put("dontUpdate", new HashMap<String, Integer>());
                    SimpleDatabase.save();
                    Toast.makeText(getActivity(), R.string.ignore_reset_done, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

}
