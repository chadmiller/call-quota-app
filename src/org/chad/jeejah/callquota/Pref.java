package org.chad.jeejah.callquota;

import android.util.Log;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;

public class Pref extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencetree);
    }

    @Override
    protected void onPause() {
        CallQuotaApplication app = (CallQuotaApplication) getApplication();

        Configuration configuration = app.conf();
        configuration.invalidate();

        UsageData usageData = app.usage();
        usageData.invalidate();

        super.onPause();
    }
}
/* vim: set et ai sta : */
