package org.chad.jeejah.callquota;

import android.util.Log;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;

public class Pref extends PreferenceActivity {
    private static final String TAG = "CallQuota.Pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencetree);

        //ListView favesList = 

        //PreferenceScreen myFavesScreen = (PreferenceScreen) findPreference("myfaves_section");
        //myFavesScreen.bind(


    }

    @Override
    protected void onPause() {
        // The preferences are already saved by this point.
        CallQuotaApplication app = (CallQuotaApplication) getApplication();

        app.invalidateAll();
        super.onStop();
    }
}
/* vim: set et ai sta : */
