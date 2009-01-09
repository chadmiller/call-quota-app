package org.chad.jeejah.callquota;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;

public class Pref extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencetree);

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(getString(R.string.pref_file));
    }

}
/* vim: set et ai sta : */
