package org.chad.jeejah.callquota;

import java.util.HashSet;
import java.util.Set;
import android.util.Log;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.provider.Settings.System;

/** 
 * General buffer to system-y things like formats for the locale, and settings
 * by the user, and (ugh) metering rules.
 */
public class Configuration {
    private static final String TAG = "CallQuota.Configuration";
    private SharedPreferences sp;

    private Context ctx;
    public Configuration(Context ctx, String owner) {
        Log.d(TAG, "Created by " + owner);
        this.ctx = ctx;
        this.sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        invalidate();
    }

/*
    public Configuration() {
        this.ctx = new FakeContextForTesting();
        this.sp = new FakeSharedPreferacnesForTesting();
        Log.d(TAG, "Bogus configuration constructed for testing.");
        invalidate();
    }

    private class FakeContextForTesting extends Context {
        public FakeContextForTesting() { }
        public String getString(int i) { return new String(i); }
    }

    private class FakeSharedPreferences extends SharedPreferences {
        public boolean getBoolean(String key, boolean defaultValue) { return defaultValue; }
        public int getInt(String key, int defaultValue) { return defaultValue; }
        public long getLong(String key, long defaultValue) { return defaultValue; }
        public String getString(String key, String defaultValue) { return defaultValue; }
    }
*/

    public void invalidate() {

        billAllowedMeteredMinutes_valid = false;
        daytimeBeginningHour_valid = false;
        daytimeEndHour_valid = false;
        firstBillDay_valid = false;
        getNumbersNeverMetered_valid = false;
        meteringOmitsWeekends_valid = false;
        meteringStartsAtCallStart_valid = false;
        wantNeverMeteredP_valid = false;
        wantNightsFree_valid = false;
        wantNotificationsP_valid = false;
        warningPercentage_valid = false;
        // :r! grep _valid\; % |awk '{ print $3 }' |sed -e 's/;/ = false;/' |sort

        Log.i(TAG, "cached data invalidated");
    }

    private boolean wantNeverMeteredP_valid;
    private boolean wantNeverMeteredP;
    public boolean getWantNeverMeteredP() {
        if (! this.wantNeverMeteredP_valid) {
            this.wantNeverMeteredP = this.sp.getBoolean(this.ctx.getString(R.string.id_some_contacts_freeP), true);
            Log.d(TAG, "refreshed wantNeverMeteredP");
            this.wantNeverMeteredP_valid = true;
        }
        return this.wantNeverMeteredP;
    }


    private boolean getNumbersNeverMetered_valid;
    HashSet<String> numbersNeverMetered = new HashSet<String>();
    Set getNumbersNeverMetered() {
        if (! getNumbersNeverMetered_valid) {
            if (getWantNeverMeteredP()) {
                numbersNeverMetered.clear();

                FreeContactsDb fcdb = new FreeContactsDb(this.ctx, "freecontacts", null, 1);
                SQLiteDatabase db = fcdb.getWritableDatabase();

                Cursor c = db.query("freecontacts", new String[] {"number", "number_key"}, "", new String[] {}, "", "", "");

                Log.d(TAG, "Getting free contact list into config.  Moving cursor to beginning.");
                if (c.moveToFirst()) {
                    int numberColumn = c.getColumnIndex("number"); 
                    int numberKeyColumn = c.getColumnIndex("number_key"); 
                    do {
                        String n = c.getString(numberKeyColumn);
                        numbersNeverMetered.add(Call.getNormalizedNumber(n));
                        Log.d(TAG, n + " -> " + Call.getNormalizedNumber(n) + "   " + numbersNeverMetered.toString());
                    } while (c.moveToNext());
                } else {
                    Log.d(TAG, "Can't seek to beginning.");
                }

                db.close();
            } else {
                Log.d(TAG, "config says not want metered.  list is empty.");
            }
        }
        getNumbersNeverMetered_valid = true;
        return numbersNeverMetered;
    }


    private boolean wantNotificationsP;
    private boolean wantNotificationsP_valid;
    boolean getWantNotificationsP() {
        if (! this.wantNotificationsP_valid) {
            this.wantNotificationsP = this.sp.getBoolean(this.ctx.getString(R.string.id_show_notificationsP), true);
            Log.d(TAG, "refreshed wantNotificationsP");
            this.wantNotificationsP_valid = true;
        }
        return this.wantNotificationsP;
    }


    private Metering meteringRules;
    public Metering getMeteringRules() {
        if (meteringRules == null) {
            this.meteringRules = (Metering) new Metering(this);
            Log.d(TAG, "refreshed meteringRules");
        }
        return this.meteringRules;
    }


    private long billAllowedMeteredMinutes;
    private boolean billAllowedMeteredMinutes_valid;
    public long getBillAllowedMeteredMinutes() {
        if (! this.billAllowedMeteredMinutes_valid) {
            this.billAllowedMeteredMinutes = Long.decode(this.sp.getString(this.ctx.getString(R.string.id_minute_limit), "400"));
            Log.d(TAG, String.format("refreshed getBillAllowedMeteredMinutes %d", billAllowedMeteredMinutes));
            this.billAllowedMeteredMinutes_valid = true;
        }
        return this.billAllowedMeteredMinutes;
    }

    
    private int warningPercentage;
    private boolean warningPercentage_valid;
    public int getWarningPercentage() {
        if (! this.warningPercentage_valid) {
            this.warningPercentage = Integer.decode(this.sp.getString(this.ctx.getString(R.string.id_warning_percentage), "90"));
            Log.d(TAG, "refreshed warningPercentage");
            this.warningPercentage_valid = true;
        }
        return this.warningPercentage;
    }


    private int firstBillDay;
    private boolean firstBillDay_valid;
    public int getFirstBillDay() {
        if (! this.firstBillDay_valid) {
            this.firstBillDay = Integer.decode(this.sp.getString(this.ctx.getString(R.string.id_first_bill_day_of_month), "15"));
            Log.d(TAG, "refreshed firstBillDay");
            this.firstBillDay_valid = true;
        }
        return this.firstBillDay;
    }


    public String getDateFormatString() {
        //this.dateFormatString = this.ctx.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        return "yyyy-MM-dd";  //  FIXME
    }


    public String getDateTimeFormatString() {
        //this.dateFormatString = this.ctx.query(System.CONTENT_URI, [System.DATE_FORMAT] ...
        return "yyyy-MM-dd HH:mm";  //  FIXME
    }


    private boolean meteringOmitsWeekends_valid;
    private boolean meteringOmitsWeekends;
    public boolean getMeteringOmitsWeekends() {
        if (! this.meteringOmitsWeekends_valid) {
            this.meteringOmitsWeekends = this.sp.getBoolean(this.ctx.getString(R.string.id_weekends_freeP), true);
            Log.d(TAG, "refreshed meteringOmitsWeekends");
            this.meteringOmitsWeekends_valid = true;
        }
        return this.meteringOmitsWeekends;
    }

    private boolean meteringStartsAtCallStart_valid;
    private boolean meteringStartsAtCallStart;
    public boolean getMeteringStartsAtCallStart() {
        if (! this.meteringStartsAtCallStart_valid) {
            this.meteringStartsAtCallStart = this.sp.getBoolean(this.ctx.getString(R.string.id_metering_starts_at_call_startP), true);
            Log.d(TAG, "refreshed meteringStartsAtCallStart");
            this.meteringStartsAtCallStart_valid = true;
        }
        return this.meteringStartsAtCallStart;
    }

    private boolean wantNightsFree_valid;
    private boolean wantNightsFree;
    public boolean getWantNightsFree() {
        if (! this.wantNightsFree_valid) {
            this.wantNightsFree = this.sp.getBoolean(this.ctx.getString(R.string.id_nights_freeP), true);
            Log.d(TAG, "refreshed wantNightsFree");
            this.wantNightsFree_valid = true;
        }
        return this.wantNightsFree;
    }

    private boolean daytimeBeginningHour_valid;
    private int daytimeBeginningHour;
    public int getDaytimeBeginningHour() {
        if (! this.daytimeBeginningHour_valid) {
            try {
                this.daytimeBeginningHour = Integer.decode(this.sp.getString(this.ctx.getString(R.string.id_daytime_beginning_hour), "7"));
            } catch (NumberFormatException e) {
                this.daytimeBeginningHour = 7;
                Log.e(TAG, "Error in format of daytimeBeginningHour.  Defaulting to " + this.daytimeBeginningHour);
            }
            Log.d(TAG, "refreshed daytimeBeginningHour" + this.daytimeBeginningHour);
            this.daytimeBeginningHour_valid = true;
        }
        return this.daytimeBeginningHour;
    }

    private boolean daytimeEndHour_valid;
    private int daytimeEndHour;
    public int getDaytimeEndHour() {
        if (! this.daytimeEndHour_valid) {
            try {
                this.daytimeEndHour = Integer.decode(this.sp.getString(this.ctx.getString(R.string.id_daytime_ending_hour), "21"));
            } catch (NumberFormatException e) {
                this.daytimeBeginningHour = 21;
                Log.e(TAG, "Error in format of daytimeEndHour.  Defaulting to " + this.daytimeEndHour);
            }
            Log.d(TAG, "refreshed daytimeEndHour"+ this.daytimeEndHour);
            this.daytimeEndHour_valid = true;
        }
        return this.daytimeEndHour;
    }



}
/* vim: set et ai sta : */
