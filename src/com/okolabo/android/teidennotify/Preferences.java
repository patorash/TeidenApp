
package com.okolabo.android.teidennotify;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final String TAG = "Preferences";

    private ListPreference mDefaultGroup;
    
    private ListPreference mDefaultAlarmTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        // 各設定を取得しておく
        mDefaultGroup = (ListPreference) getPreferenceScreen().findPreference(
                Prefs.KEY_DEFAULT_GROUP);
        mDefaultAlarmTime = (ListPreference) getPreferenceScreen().findPreference(
                Prefs.KEY_DEFAULT_ALARM_TIME);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Resources r = getResources();
        String[] keys = null;
        String[] values = null;
        if (key.equals(Prefs.KEY_DEFAULT_GROUP)) {
            values = r.getStringArray(R.array.default_group_values);
            for (int i = 0; i < values.length; ++i) {
                if (sharedPreferences.getString(key, "").equals(values[i])) {
                    keys = r.getStringArray(R.array.default_groups);
                    mDefaultGroup.setSummary(keys[i]);
                    break;
                }
            }
        } else if (key.equals(Prefs.KEY_DEFAULT_ALARM_TIME)) {
            values = r.getStringArray(R.array.default_alarm_time_values);
            for (int i = 0; i < values.length; ++i) {
                if (sharedPreferences.getString(key, "10").equals(values[i])) {
                    keys = r.getStringArray(R.array.default_alarm_time);
                    mDefaultAlarmTime.setSummary(keys[i]);
                    break;
                }
            }
            
        }
    }

    @Override
    protected void onResume() {
        Resources r = getResources();
        String[] keys = null;
        String[] values = null;
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // 既定のグループ
        values = r.getStringArray(R.array.default_group_values);
        for (int i = 0; i < values.length; ++i) {
            if (sharedPreferences.getString(Prefs.KEY_DEFAULT_GROUP, "").equals(values[i])) {
                keys = r.getStringArray(R.array.default_groups);
                mDefaultGroup.setSummary(keys[i]);
                break;
            }
        }
        // 既定のアラーム時間
        values = r.getStringArray(R.array.default_alarm_time_values);
        for (int i = 0; i < values.length; ++i) {
            if (sharedPreferences.getString(Prefs.KEY_DEFAULT_ALARM_TIME, "10").equals(values[i])) {
                keys = r.getStringArray(R.array.default_alarm_time);
                mDefaultAlarmTime.setSummary(keys[i]);
                break;
            }
        }
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }
}
