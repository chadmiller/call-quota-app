package org.chad.jeejah.callquota;

import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog.Calls;

public class UsageData {
    private static final String TAG = "UsageData";

    public static Snapshot snapshot;

    public class Snapshot {
        public final long usedTotalMinutes;
        public final long usedTotalMeteredMinutes;
        public final long beginningOfPeriodAsMs;
        public final long endOfPeriodAsMs;
        public final long timestamp;

        public Snapshot(
                long usedTotalMinutes,
                long usedTotalMeteredMinutes,
                long beginningOfPeriodAsMs,
                long endOfPeriodAsMs
                ) {
            this.usedTotalMinutes = usedTotalMinutes;
            this.usedTotalMeteredMinutes = usedTotalMeteredMinutes;
            this.beginningOfPeriodAsMs = beginningOfPeriodAsMs;
            this.endOfPeriodAsMs = endOfPeriodAsMs;
            this.timestamp = java.lang.System.currentTimeMillis();
        }
    };
    

    public static Call[] getCalls(Configuration configuration, Context context) {
        Log.d(TAG, "refresh()");

        String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER };
        Cursor cursor;

		ContentResolver cr = context.getContentResolver();
        cursor = cr.query(Calls.CONTENT_URI, projection, null, null, null);

        long usedTotalMeteredMinutes = 0;
        long usedTotalMinutes = 0;

        Call[] callList = null;

        callList = new Call[cursor.getCount()];
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
                callList[i] = new Call(dateInMs / 1000, (dateInMs / 1000) + (durationSeconds / 60), meteredMinutes, false);
                usedTotalMinutes += minutes;
                usedTotalMeteredMinutes += meteredMinutes;

                //lastCall = thisCall;
                i++;
            } while (cursor.moveToNext());

        } else {
            Log.d(TAG, "The provider is empty.  That's okay.");
        }

        Snapshot newSnapshot = new Snapshot(usedTotalMinutes, usedTotalMeteredMinutes, configuration.meteringRules.getPeriodStart(), configuration.meteringRules.getPeriodEnd());

        return callList;
    }


}
/* vim: set et ai sta : */
