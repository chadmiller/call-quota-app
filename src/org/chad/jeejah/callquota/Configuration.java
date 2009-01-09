package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.provider.Settings.System;
import org.chad.jeejah.callquota.carrier.*;

public class Configuration {
    private static final String TAG = "Configuration";

    public int warningPercentage;
    public long billAllowedMeteredMinutes;
    public boolean runUnitTestsP;
    public Class meteringRulesClass;
    public static AllMetered meteringRules;
    public String dateFormatString;
    public int firstBillDay;

    private Context storedContext;

    public void load(Context context) {
        Log.d(TAG, "load()");
        this.storedContext = context;
        refresh();
    }

    public void refresh() {
        Log.d(TAG, "refresh()");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.storedContext);
        //context.getSharedPreferences(context.getString(R.string.pref_file), 0);

        // create meter
        String confMeterName = settings.getString(this.storedContext.getString(R.string.pref_carrier_rules), "Tmobile");
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
        this.billAllowedMeteredMinutes = Long.parseLong(settings.getString(this.storedContext.getString(R.string.pref_minute_limit), "400"));
        this.warningPercentage = settings.getInt("warningPercentage", 90);
        this.firstBillDay = Integer.parseInt(settings.getString(this.storedContext.getString(R.string.pref_first_bill_day_of_month), "15"));

        //this.dateFormatString = this.storedContext.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        this.dateFormatString = "yyyy-MM-dd";

        Log.d(TAG, String.format("confMeterName is %s and billAllowedMeteredMinutes is %d and firstBillDay is %d", confMeterName, this.billAllowedMeteredMinutes, this.firstBillDay));
    }
}
/* vim: set et ai sta : */
