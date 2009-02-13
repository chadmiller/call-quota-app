package org.chad.jeejah.callquota;

import android.util.Log;
import android.util.TimingLogger;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog.Calls;

import java.util.List;
import java.util.ArrayList;

public class UsageData {
    private static final String TAG = "CallQuota.UsageData";

    private int historicalCallCount;
    public boolean getIsSufficientDataToPredictP() {
        if (! valid)
            getCallList(); // has side effects

        if (historicalCallCount > 10) {
            return true;
        }

        if (isCurrent()) {
            long now = java.lang.System.currentTimeMillis();
            long perBeg = getBeginningOfPeriodAsMs();
            long perEnd = getEndOfPeriodAsMs();

            float passed = (float)(now - perBeg) / (float)(perEnd - perBeg);
            if (passed > 0.2) {
                return true;
            }
        } else {
            Log.w(TAG, "getIsSufficientDataToPredictP(): not the current period.  FIXME!");  // FIXME!
        }

        return false;
    }

    public boolean isCurrent() {
        return this.nthMonthBack == 0;
    }

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

    private long usedTotalMeteredMinutesLastMonth;
    public long getUsedTotalMeteredMinutesLastMonth() {
        if (! valid)
            getCallList(); // has side effects
        return this.usedTotalMeteredMinutesLastMonth;
    }

    private boolean beginningOfHistoryAsMs_valid;
    private long beginningOfHistoryAsMs;
    public long getBeginningOfHistoryAsMs() {
        if (! beginningOfHistoryAsMs_valid) {
            this.beginningOfHistoryAsMs = this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(this.nthMonthBack+2, this.configuration.getFirstBillDay());
            beginningOfHistoryAsMs_valid = true;
            Log.d(TAG, "refreshed beginningOfHistoryAsMs = " + this.beginningOfHistoryAsMs);
        }
        return this.beginningOfHistoryAsMs;
    }

    private boolean beginningOfPeriodAsMs_valid;
    private long beginningOfPeriodAsMs;
    public long getBeginningOfPeriodAsMs() {
        if (! beginningOfPeriodAsMs_valid) {
            this.beginningOfPeriodAsMs = this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(this.nthMonthBack+1, this.configuration.getFirstBillDay());
            beginningOfPeriodAsMs_valid = true;
            Log.d(TAG, "refreshed beginningOfPeriodAsMs = " + this.beginningOfPeriodAsMs);
        }
        return this.beginningOfPeriodAsMs;
    }

    private boolean endOfPeriodAsMs_valid;
    private long endOfPeriodAsMs;
    public long getEndOfPeriodAsMs() {
        if (! endOfPeriodAsMs_valid) {
            this.endOfPeriodAsMs = this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(this.nthMonthBack, this.configuration.getFirstBillDay());
            endOfPeriodAsMs_valid = true;
            Log.d(TAG, "refreshed endOfPeriodAsMs = " + this.endOfPeriodAsMs);
        }
        return this.endOfPeriodAsMs;
    }

    private long predictionAtBillMinutes;
    /** Prediction is never cached, as it depends on the current time. */
    public long getPredictionAtBillMinutes() {
        if (! isCurrent()) {
            Log.d(TAG, "getPredictionAtBillMinutes():  Since we're not current, instead of predicting, we just return what happened.  The caller should be smarter.");
            return getUsedTotalMeteredMinutes();
        }

        if (! valid)
            getCallList(); // has side effects

        try {
            long nowSec = java.lang.System.currentTimeMillis() / 1000;
            long periodLength = nowSec - (getBeginningOfHistoryAsMs() / 1000);

            double growthInPeriod = (double) (getUsedTotalMeteredMinutesLastMonth() + getUsedTotalMeteredMinutes());
            double growthRate = growthInPeriod / periodLength;

            long predictionPeriod = getEndOfPeriodAsMs()/1000 - nowSec;
            this.predictionAtBillMinutes = (long) (growthRate * predictionPeriod);
            this.predictionAtBillMinutes += getUsedTotalMeteredMinutes();

            return this.predictionAtBillMinutes;

        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "There were no call entries to scan.");
            throw e;
        }
    }

    private Configuration configuration;
    private Context context;
    private int nthMonthBack;
    UsageData(Context context, Configuration configuration, String owner, int nthMonthBack) {
        this.context = context;
        this.configuration = configuration;
        this.nthMonthBack = nthMonthBack;

        invalidate();
    }

    boolean valid;
    public void invalidate() {
        this.valid = false;
        this.endOfPeriodAsMs_valid = false;
        this.beginningOfPeriodAsMs_valid = false;
        this.beginningOfHistoryAsMs_valid = false;
        Log.i(TAG, "cached data invalidated");
    }

    
    private List<Call> callList;
    public List<Call> getCallList() {
        TimingLogger tl = new TimingLogger(TAG, "getCallList()");
        
        try {

            if (valid) {
                return callList;
            }

            String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER };
            Cursor cursor;
            
            ContentResolver cr = this.context.getContentResolver();
            String whereClause = String.format("(%1$s + (%4$s * 1000)) > %2$d and (%1$s + (%4$s * 1000)) <= %3$d", Calls.DATE, getBeginningOfHistoryAsMs(), getEndOfPeriodAsMs(), Calls.DURATION);
            cursor = cr.query(Calls.CONTENT_URI, projection, whereClause, null, Calls.DATE);
            try {

                this.usedTotalMeteredMinutesLastMonth = 0;
                this.usedTotalMeteredMinutes = 0;
                this.usedTotalMinutes = 0;
                this.historicalCallCount = 0;
                List<Call> newCallList = new ArrayList<Call>(cursor.getCount());

                if (cursor.moveToFirst()) {
                    int dateColumn = cursor.getColumnIndex(Calls.DATE); 
                    int durationColumn = cursor.getColumnIndex(Calls.DURATION);
                    int typeColumn = cursor.getColumnIndex(Calls.TYPE);
                    int numberColumn = cursor.getColumnIndex(Calls.NUMBER);

                    String phoneNumber; 

                    do {
                        long dateInMs, durationSeconds;
                        int type;
                        String number;

                        dateInMs = cursor.getLong(dateColumn);
                        durationSeconds = cursor.getLong(durationColumn);
                        type = cursor.getInt(typeColumn);
                        number = cursor.getString(numberColumn);

                        boolean isHistoricalP = dateInMs+(durationSeconds*1000) < getBeginningOfPeriodAsMs();
                        
                        Call c = configuration.getMeteringRules().recordCallInfo(dateInMs, durationSeconds, number, type);

                        if (isHistoricalP) {
                            this.historicalCallCount++;
                            this.usedTotalMeteredMinutesLastMonth += c.meteredMinutes;

                        } else {
                            long dateInSec = (long) Math.ceil(dateInMs / 1000.0);

                            newCallList.add(c);

                            this.usedTotalMinutes += (long) Math.ceil(durationSeconds / 60.0);
                            this.usedTotalMeteredMinutes += c.meteredMinutes;
                        }

                    } while (cursor.moveToNext());

                } else {
                    Log.d(TAG, "The provider is empty.  That's okay.");
                }
                this.callList = newCallList;

                Log.d(TAG, "refreshed usedTotalMinutes, usedTotalMeteredMinutes, callList");
                valid = true;
                return this.callList;
            } finally {
                cursor.close();
            }
        } finally {
            assert this.callList != null;
        }
    }

}
/* vim: set et ai sta : */
