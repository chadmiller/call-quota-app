package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.provider.Settings.System;
import org.chad.jeejah.callquota.carrier.*;
import org.chad.jeejah.callquota.carrier.Tmobile;

public class Configuration {
    private static final String TAG = "Configuration";

    public int warningPercentage;
    public long billAllowedMeteredMinutes;
    public boolean runUnitTestsP;
    public Class meteringRulesClass;
    public static AllMetered meteringRules;
    public String dateFormatString;
    public int firstBillDay;
    public boolean postNotificationsP;

    private Context storedContext;

    public void load(Context context) {
        Log.d(TAG, "load()");
        this.storedContext = context;
        refresh();
    }

    public void refresh() {
        Log.d(TAG, "refresh()");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.storedContext);

        // create meter
        String confMeterName = settings.getString(this.storedContext.getString(R.string.id_carrier_rules), "Tmobile");
        try {
            this.meteringRulesClass = Class.forName("org.chad.jeejah.callquota.carrier." + confMeterName);
            this.meteringRules = (AllMetered) this.meteringRulesClass.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException");
            this.meteringRules = (AllMetered) new Tmobile();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException");
            this.meteringRules = (AllMetered) new Tmobile();
        } catch (InstantiationException e) {
            Log.e(TAG, "InstantiationException");
            this.meteringRules = (AllMetered) new Tmobile();
        }

        this.runUnitTestsP = settings.getBoolean("runUnitTests", false);

        this.postNotificationsP = settings.getBoolean(this.storedContext.getString(R.string.id_show_notifications), true);
        this.billAllowedMeteredMinutes = Long.parseLong(settings.getString(this.storedContext.getString(R.string.id_minute_limit), "400"));
        this.warningPercentage = settings.getInt("warningPercentage", 90);
        this.firstBillDay = Integer.parseInt(settings.getString(this.storedContext.getString(R.string.id_first_bill_day_of_month), "15"));

        //this.dateFormatString = this.storedContext.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        this.dateFormatString = "yyyy-MM-dd";  //  FIXME

        Log.d(TAG, String.format("confMeterName is %s and billAllowedMeteredMinutes is %d and firstBillDay is %d", confMeterName, this.billAllowedMeteredMinutes, this.firstBillDay));
    }
}
/* vim: set et ai sta : */
