package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;

import android.provider.Settings.System;
import org.chad.jeejah.callquota.carrier.*;

public class Configuration {
    private static final String TAG = "Configuration";
    private static final String PREFS_NAME = "CallQuota.root";

    public int warningPercentage;
    public long billAllowedMeteredMinutes;
    public boolean runUnitTestsP;
    public Class meteringRulesClass;
    public static AllMetered meteringRules;
    public String dateFormatString;

    public void load(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

        // create meter
        String confMeterName = settings.getString("meteringRules", "Tmobile");
        try {
            this.meteringRulesClass = Class.forName("org.chad.jeejah.callquota.carrier." + confMeterName);
            this.meteringRules = (AllMetered) this.meteringRulesClass.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException");
        } catch (InstantiationException e) {
            Log.e(TAG, "InstantiationException");
        }

        this.runUnitTestsP = settings.getBoolean("runUnitTests", false);
        this.billAllowedMeteredMinutes = settings.getLong("billAllowedMeteredMinutes", 400);
        this.warningPercentage = settings.getInt("billAllowedMeteredMinutes", 90);

        //this.dateFormatString = context.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        this.dateFormatString = "yyyy-MM-dd";

    }
}
/* vim: set et ai sta : */
