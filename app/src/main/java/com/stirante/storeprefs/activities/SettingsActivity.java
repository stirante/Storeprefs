package com.stirante.storeprefs.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.stirante.storeprefs.Commons;
import com.stirante.storeprefs.R;
import com.stirante.storeprefs.utils.AppInfo;
import com.stirante.storeprefs.utils.SimpleDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


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
            findPreference("reset_ignore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SimpleDatabase.load();
                    SimpleDatabase.put(Commons.DATA_DONT_UPDATE, new HashMap<String, Integer>());
                    SimpleDatabase.save();
                    Toast.makeText(getActivity(), R.string.ignore_reset_done, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            findPreference("save_state").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SimpleDatabase.load();
                    ArrayList<AppInfo> apps = (ArrayList<AppInfo>) SimpleDatabase.get(Commons.DATA_RESTORE, new ArrayList<AppInfo>());
                    apps.clear();
                    List<PackageInfo> applications = getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_CONFIGURATIONS);
                    for (PackageInfo info : applications) {
                        AppInfo app = new AppInfo();
                        app.packageName = info.packageName;
                        app.versionCode = info.versionCode;
                        app.title = info.packageName;
                        apps.add(app);
                    }
                    SimpleDatabase.save();
                    Toast.makeText(getActivity(), R.string.save_done, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            findPreference("restore_state").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    restore();
                    Toast.makeText(getActivity(), R.string.restore_doing, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            /**
             findPreference("trigger_experiment").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
            AppInfo app = new AppInfo();
            app.packageName = "com.skype.raider";
            app.versionCode = 84010293;
            app.title = "Skype";
            install(app);
            return true;
            }
            });**/
            IntentFilter filter = new IntentFilter(Commons.ACTION_INSTALL_RESULT);
            getActivity().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getExtras() != null && intent.getExtras().getString("myId") != null && intent.getExtras().getString("myId").equalsIgnoreCase("just to identify which intents are mine")) {
                        if (intent.getExtras().getBoolean("success"))
                            Toast.makeText(getActivity(), R.string.installed, Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getActivity(), R.string.not_installed, Toast.LENGTH_SHORT).show();
                    }
                }
            }, filter);
        }

        public void install(String packageName, int versionCode, String title) {
            Intent intent = new Intent(Commons.ACTION_INSTALL);//com.stirante.storeprefs.INSTALL
            intent.putExtra("packageName", packageName);
            intent.putExtra("title", title);
            intent.putExtra("versionCode", versionCode);
            intent.putExtra("myId", "just to identify which intents are mine");//this is not required. INSTALL_RESULT sends the same exras so i can identify which intents are mine
            getActivity().sendBroadcast(intent);
        }

        public void restore() {
            Intent intent = new Intent(Commons.ACTION_RESTORE);
            intent.putExtra("myId", "just to identify which intents are mine");
            getActivity().sendBroadcast(intent);
        }

        public void install(AppInfo app) {
            install(app.packageName, app.versionCode, app.title);
        }
    }

}
