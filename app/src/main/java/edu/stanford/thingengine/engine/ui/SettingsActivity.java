// This file is part of Almond
//
// Copyright 2016-2017 The Board of Trustees of the Leland Stanford Junior University
//
// See COPYING for details
//
package edu.stanford.thingengine.engine.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import edu.stanford.thingengine.engine.CloudAuthInfo;
import edu.stanford.thingengine.engine.R;

public class SettingsActivity extends Activity {
    private final EngineServiceConnection mEngine = new EngineServiceConnection();
    private CloudAuthInfo mAuthInfo;
    private String mDeveloperKey;

    private PreferenceFragment mFragment;

    private final SharedPreferences.OnSharedPreferenceChangeListener mThingenginePrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("cloud-id") || key.equals("auth-token") || key.equals("developer-key"))
                initAuthInfo();
        }
    };

    public static class Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screen);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        mFragment = ((PreferenceFragment) getFragmentManager().findFragmentById(R.id.settings_fragment));
        initAuthInfo();

        SharedPreferences prefs = getSharedPreferences("thingengine", Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(mThingenginePrefListener);

        mFragment.findPreference("pref_cloud_sync").setIntent(new Intent(this, ThingpediaWebsiteActivity.class));

        final Preference pref_landing_page = mFragment.findPreference("pref_landing_page");
        pref_landing_page.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                SharedPreferences sharedPrefs = getSharedPreferences("edu.stanford.thingengine.engine", MODE_PRIVATE);
                sharedPrefs.edit().putBoolean("landing-page", (Boolean) o).apply();
                return true;
            }
        });

        final Preference pref_store_log = mFragment.findPreference("pref_store_log");
        pref_store_log.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                SharedPreferences sharedPrefs = getSharedPreferences("thingengine", MODE_PRIVATE);
                sharedPrefs.edit().putString("sabrina-store-log", ((Boolean) o) ? "\"yes\"" : "\"no\"").apply();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = getSharedPreferences("thingengine", Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(mThingenginePrefListener);
    }

    private String readStringPref(SharedPreferences prefs, String key) {
        try {
            // shared preferences have one extra layer of json that we need to unwrap
            Object obj = new JSONTokener(prefs.getString(key, "null")).nextValue();
            return obj == JSONObject.NULL ? null : (String) obj;
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void initAuthInfo() {
        SharedPreferences prefs = getSharedPreferences("thingengine", Context.MODE_PRIVATE);

        String cloudId = readStringPref(prefs, "cloud-id");
        String authToken = readStringPref(prefs, "auth-token");
        String developerKey = readStringPref(prefs, "developer-key");

        mAuthInfo = new CloudAuthInfo(cloudId, authToken);
        mDeveloperKey = developerKey;

        refreshView();
    }

    private void refreshView() {
        CloudAuthInfo info = mAuthInfo;

        Preference pref = mFragment.findPreference("pref_cloud_sync");
        if (info.isValid())
            pref.setSummary(R.string.cloud_sync_enabled);
        else
            pref.setSummary(R.string.cloud_sync_disabled);

        pref = mFragment.findPreference("pref_developer_key");
        if (mDeveloperKey != null)
            pref.setSummary(mDeveloperKey);
        else
            pref.setSummary(R.string.no_developer_key);

        SharedPreferences prefs = getSharedPreferences("thingengine", Context.MODE_PRIVATE);
        SharedPreferences appPrefs = mFragment.getPreferenceManager().getSharedPreferences();

        appPrefs.edit().putBoolean("pref_store_log", "yes".equals(readStringPref(prefs, "sabrina-store-log"))).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mEngine.start(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mEngine.stop(this);
    }
}
