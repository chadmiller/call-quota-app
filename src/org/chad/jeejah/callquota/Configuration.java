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
    private SharedPreferences sp;

    private Context ctx;
    public Configuration(Context ctx, String owner) {
        Log.d(TAG, "Created by " + owner);

        this.ctx = ctx;
        this.sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        invalidate();
    }


    public void invalidate() {
        billAllowedMeteredMinutes_valid = false;
        firstBillDay_valid = false;
        meteringRulesClass_valid = false;
        wantNotificationsP_valid = false;
        wantUnitTestsP_valid = false;
        warningPercentage_valid = false;

        // :r! grep _valid\; % |awk '{ print $3 }' |sed -e 's/;/ = false;/' |sort

        Log.i(TAG, "cached data invalidated");
    }


    private boolean wantNotificationsP;
    private boolean wantNotificationsP_valid;
    boolean getWantNotificationsP() {
        if (! this.wantNotificationsP_valid) {
            this.wantNotificationsP = this.sp.getBoolean(this.ctx.getString(R.string.id_show_notifications), true);
            Log.d(TAG, "refreshed wantNotificationsP");
            this.wantNotificationsP_valid = true;
        }
        return this.wantNotificationsP;
    }

    private Class meteringRulesClass;
    private boolean meteringRulesClass_valid;
    public Class getMeteringRulesClass() {
        if (! meteringRulesClass_valid) {
            String confMeterName = this.sp.getString(this.ctx.getString(R.string.id_carrier_rules), "Tmobile");
            try {
                this.meteringRulesClass = Class.forName("org.chad.jeejah.callquota.carrier." + confMeterName);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "ClassNotFoundException");
                return null;
            }
            Log.d(TAG, "refreshed meteringRulesClass");
            meteringRulesClass_valid = true;
        }
        return meteringRulesClass;
    }

    private AllMetered meteringRules;
    public AllMetered getMeteringRules() {
        if (meteringRules == null) {
            try {
                Class c = getMeteringRulesClass();
                if (c != null)
                    this.meteringRules = (AllMetered) c.newInstance();
                else {
                    this.meteringRules = (AllMetered) new Tmobile();
                    Log.e(TAG, "None found.  Falling back to " + this.meteringRules.TAG);
                }
            } catch (IllegalAccessException e) {
                this.meteringRules = (AllMetered) new Tmobile();
                Log.e(TAG, "IllegalAccessException.  Falling back to " + this.meteringRules.TAG);
            } catch (InstantiationException e) {
                this.meteringRules = (AllMetered) new Tmobile();
                Log.e(TAG, "InstantiationException.  Falling back to " + this.meteringRules.TAG);
            }
            Log.d(TAG, "refreshed meteringRules");
        }
        return this.meteringRules;
    }


    private boolean wantUnitTestsP;
    private boolean wantUnitTestsP_valid;
    public boolean getWantUnitTestsP() {
        if (! this.wantUnitTestsP_valid) {
            this.wantUnitTestsP = this.sp.getBoolean("wantUnitTests", false);
            Log.d(TAG, "refreshed wantUnitTestsP");
            this.wantUnitTestsP_valid = true;
        }
        return this.wantUnitTestsP;
    }


    private long billAllowedMeteredMinutes;
    private boolean billAllowedMeteredMinutes_valid;
    public long getBillAllowedMeteredMinutes() {
        if (! this.billAllowedMeteredMinutes_valid) {
            String s = this.sp.getString(this.ctx.getString(R.string.id_minute_limit), "400");
            this.billAllowedMeteredMinutes = Long.parseLong(s);
            Log.d(TAG, String.format("refreshed getBillAllowedMeteredMinutes %d", billAllowedMeteredMinutes));
            this.billAllowedMeteredMinutes_valid = true;
        }
        return this.billAllowedMeteredMinutes;
    }

    
    private int warningPercentage;
    private boolean warningPercentage_valid;
    public int getWarningPercentage() {
        if (! this.warningPercentage_valid) {
            this.warningPercentage = this.sp.getInt("warningPercentage", 90);
            Log.d(TAG, "refreshed warningPercentage");
            this.warningPercentage_valid = true;
        }
        return this.warningPercentage;
    }


    private int firstBillDay;
    private boolean firstBillDay_valid;
    public int getFirstBillDay() {
        if (! this.firstBillDay_valid) {
            this.firstBillDay = Integer.parseInt(this.sp.getString(this.ctx.getString(R.string.id_first_bill_day_of_month), "15"));
            Log.d(TAG, "refreshed firstBillDay");
            this.firstBillDay_valid = true;
        }
        return this.firstBillDay;
    }


    public String getDateFormatString() {
        //this.dateFormatString = this.ctx.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        return "yyyy-MM-dd";  //  FIXME
    }
}
/* vim: set et ai sta : */
