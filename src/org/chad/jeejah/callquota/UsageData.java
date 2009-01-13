package org.chad.jeejah.callquota;

import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog.Calls;

public class UsageData {
    private static final String TAG = "UsageData";

    private long usedTotalMinutes;
    public long getUsedTotalMinutes() {
        if (! valid)
            getCallList(); // has side effects
        return this.usedTotalMinutes;
    }

    private long usedTotalMeteredMinutes;
    public long getUsedTotalMeteredMinutes() {
        if (! valid)
            getCallList(); // has side effects
        return this.usedTotalMeteredMinutes;
    }

    private boolean beginningOfPeriodAsMs_valid;
    private long beginningOfPeriodAsMs;
    public long getBeginningOfPeriodAsMs() {
        if (! beginningOfPeriodAsMs_valid) {
            this.beginningOfPeriodAsMs = this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(1, this.configuration.getFirstBillDay());
            beginningOfPeriodAsMs_valid = true;
            Log.d(TAG, "refreshed beginningOfPeriodAsMs");
        }
        return this.beginningOfPeriodAsMs;
    }

    private boolean endOfPeriodAsMs_valid;
    private long endOfPeriodAsMs;
    public long getEndOfPeriodAsMs() {
        if (! endOfPeriodAsMs_valid) {
            this.endOfPeriodAsMs = this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(0, this.configuration.getFirstBillDay());
            endOfPeriodAsMs_valid = true;
            Log.d(TAG, "refreshed endOfPeriodAsMs");
        }
        return this.endOfPeriodAsMs;
    }

    private long predictionAtBillMinutes;
    /** Prediction is never cached, as it depends on the current time. */
    public long getPredictionAtBillMinutes() {
        if (! valid)
            getCallList(); // has side effects

        try {
            long nowSec = java.lang.System.currentTimeMillis() / 1000;
            long finalPointSec = this.callList[this.callList.length-1].endFromEpochSec;
            long periodLength = finalPointSec - (getBeginningOfPeriodAsMs() / 1000);
            double growthInPeriod = (double) getUsedTotalMeteredMinutes();
            double growthRate = growthInPeriod / periodLength;
            long predictionPeriod = (getEndOfPeriodAsMs() - getBeginningOfPeriodAsMs()) / 1000;
            this.predictionAtBillMinutes = (long) (growthRate * predictionPeriod);

            return this.predictionAtBillMinutes;

        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "There were no call entries to scan.");
            throw e;
        }
    }

    private Configuration configuration;
    private Context context;
    UsageData(Context context, Configuration configuration, String owner) {
        this.context = context;
        this.configuration = configuration;

        invalidate();
    }

    boolean valid;
    public void invalidate() {
        this.valid = false;
        this.endOfPeriodAsMs_valid = false;
        this.beginningOfPeriodAsMs_valid = false;
        Log.i(TAG, "cached data invalidated");
    }

    
    private Call[] callList;
    public Call[] getCallList() {
        if (valid)
            return callList;

        Call[] newCallList = null;
        String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER };
        Cursor cursor;
        
		ContentResolver cr = this.context.getContentResolver();
        String whereClause = String.format("(%1$s + (%4$s * 1000)) > %2$d and (%1$s + (%4$s * 1000)) <= %3$d", Calls.DATE, getBeginningOfPeriodAsMs(), getEndOfPeriodAsMs(), Calls.DURATION);
        cursor = cr.query(Calls.CONTENT_URI, projection, whereClause, null, null);

        this.usedTotalMeteredMinutes = 0;
        this.usedTotalMinutes = 0;
        newCallList = new Call[cursor.getCount()];

        if (cursor.moveToFirst()) {
            int dateColumn = cursor.getColumnIndex(Calls.DATE); 
            int durationColumn = cursor.getColumnIndex(Calls.DURATION);
            int typeColumn = cursor.getColumnIndex(Calls.TYPE);
            int numberColumn = cursor.getColumnIndex(Calls.NUMBER);

            String phoneNumber; 

            int i = 0;
            do {
                long dateInMs, durationSeconds;
                int type;
                String number;

                dateInMs = cursor.getLong(dateColumn);
                durationSeconds = cursor.getLong(durationColumn);
                type = cursor.getInt(typeColumn);
                number = cursor.getString(numberColumn);

                long meteredMinutes = (long) Math.ceil(configuration.getMeteringRules().extractMeteredSeconds(dateInMs, durationSeconds, number, type) / 60.0);

                assert(callList.length < i);

                long dateInSec = (long) Math.ceil(dateInMs / 1000.0);
                newCallList[i] = new Call(dateInSec, dateInSec+durationSeconds, meteredMinutes, number);

                this.usedTotalMinutes += (long) Math.ceil(durationSeconds / 60.0);
                this.usedTotalMeteredMinutes += meteredMinutes;

                i++;
            } while (cursor.moveToNext());

        } else {
            Log.d(TAG, "The provider is empty.  That's okay.");
        }
        this.callList = newCallList;

        Log.d(TAG, "refreshed usedTotalMinutes, usedTotalMeteredMinutes, callList");
        valid = true;
        return this.callList;
    }


}
/* vim: set et ai sta : */
