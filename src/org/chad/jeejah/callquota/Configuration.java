package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;

import org.chad.jeejah.callquota.carrier.*;

public class Configuration {
    private static final String TAG = "Configuration";
    private static final String PREFS_NAME = "CallQuota.root";

    public int warningPercentage;
    public long billAllowedMeteredMinutes;
    public boolean runUnitTestsP;
    public static AllMetered meteringRules;

    public void load(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

        // create meter
        String confMeterName = settings.getString("meteringRules", "HoursRestricted");
        Class meterClass = null;
        try {
            meterClass = Class.forName("org.chad.jeejah.callquota.carrier." + confMeterName);
            this.meteringRules = (AllMetered) meterClass.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException");
        } catch (InstantiationException e) {
            Log.e(TAG, "InstantiationException");
        }

        this.runUnitTestsP = settings.getBoolean("runUnitTests", false);
        this.billAllowedMeteredMinutes = settings.getLong("billAllowedMeteredMinutes", 400);

        /*
        if (runUnitTestsP) {
            AndroidRunner runner = new AndroidRunner(new SoloRunner());
            runner.run(meterClass);
        }
        */

    }
}
/* vim: set et ai sta : */
