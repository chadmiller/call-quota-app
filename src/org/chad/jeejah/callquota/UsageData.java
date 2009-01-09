package org.chad.jeejah.callquota;

import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog.Calls;

public class UsageData {
    private static final String TAG = "UsageData";

    public long usedTotalMinutes;
    public long usedTotalMeteredMinutes;
    public long beginningOfPeriodAsMs;
    public long endOfPeriodAsMs;
    public long timestamp;
    public long predictionAtBillMinutes;
    public Call[] callList;

    private Configuration configuration;
    private Context context;

    UsageData(Context context, Configuration configuration) {
        this.context = context;
        this.configuration = configuration;
    }

    public void scanLog(boolean storeIndividualCalls) {
        Call[] newCallList = null;
        Log.d(TAG, "scanLog()");

        this.configuration.refresh();

        String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER };
        Cursor cursor;


        int firstBillDay = configuration.firstBillDay;

		ContentResolver cr = context.getContentResolver();
        cursor = cr.query(Calls.CONTENT_URI, projection, String.format("(%1$s + (%4$s * 1000)) > %2$d and (%1$s + (%4$s * 1000)) <= %3$d", Calls.DATE, configuration.meteringRules.getEndOfNthBillBackAsMs(1, firstBillDay), configuration.meteringRules.getEndOfNthBillBackAsMs(0, firstBillDay), Calls.DURATION), null, null);
        // Find where end of call is in billing period.

        long usedTotalMeteredMinutes = 0;
        long usedTotalMinutes = 0;

        if (storeIndividualCalls) {
            newCallList = new Call[cursor.getCount()];
        }

        if (cursor.moveToFirst()) {
            int dateColumn = cursor.getColumnIndex(Calls.DATE); 
            int durationColumn = cursor.getColumnIndex(Calls.DURATION);
            int typeColumn = cursor.getColumnIndex(Calls.TYPE);
            int numberColumn = cursor.getColumnIndex(Calls.NUMBER);

            String phoneNumber; 
            UsageData usage;
            //GregorianCalendar thisCall, lastCall = null;

            int i = 0;
            do {
                long dateInMs, durationSeconds;
                int type;
                String number;

                dateInMs = cursor.getLong(dateColumn);
                durationSeconds = cursor.getLong(durationColumn);
                type = cursor.getInt(typeColumn);
                number = cursor.getString(numberColumn);

                //thisCall.setTimeInMillis(dateInMs);

                long minutes = durationSeconds / 60;
                long meteredMinutes = configuration.meteringRules.extractMeteredSeconds(dateInMs, durationSeconds, number, type) / 60;

                assert(callList.length < i);
                if (storeIndividualCalls)
                    newCallList[i] = new Call(dateInMs / 1000, (dateInMs / 1000) + (durationSeconds / 60), meteredMinutes, false);
                usedTotalMinutes += minutes;
                usedTotalMeteredMinutes += meteredMinutes;

                //lastCall = thisCall;
                i++;
            } while (cursor.moveToNext());

        } else {
            Log.d(TAG, "The provider is empty.  That's okay.");
        }


        if (storeIndividualCalls)
            this.callList = newCallList;

        double prediction = 0.0;

        if ((newCallList != null) && (newCallList.length > 1)) {
            long nowSec = java.lang.System.currentTimeMillis() / 1000;
            long graphBeginningOfTimeSec = this.configuration.meteringRules.getEndOfNthBillBackAsMs(1, firstBillDay) / 1000;
            long graphEndOfTimeSec = configuration.meteringRules.getEndOfNthBillBackAsMs(0, firstBillDay) / 1000;

            long finalPointSec = newCallList[newCallList.length-1].endFromEpochSec;
            
            long periodLength = finalPointSec - graphBeginningOfTimeSec;
            long growthInPeriod = usedTotalMeteredMinutes;

            double growthRate = (double) growthInPeriod / periodLength;

            long predictionPoint = graphEndOfTimeSec;

            long predictionPeriod = graphEndOfTimeSec - graphBeginningOfTimeSec;

            prediction = growthRate * predictionPeriod;
        }

        this.usedTotalMinutes = usedTotalMinutes;
        this.usedTotalMeteredMinutes = usedTotalMeteredMinutes;
        this.beginningOfPeriodAsMs = beginningOfPeriodAsMs;
        this.endOfPeriodAsMs = endOfPeriodAsMs;
        this.timestamp = java.lang.System.currentTimeMillis();
        this.predictionAtBillMinutes = (long) prediction;

    }


}
/* vim: set et ai sta : */
