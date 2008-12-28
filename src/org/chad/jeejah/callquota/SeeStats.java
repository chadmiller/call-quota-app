package org.chad.jeejah.callquota;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.widget.TextView;

import android.database.Cursor;
import android.provider.CallLog.Calls;

import org.chad.jeejah.callquota.carrier.*;

public class SeeStats extends Activity
{
    static final String TAG = "SeeStats";

    String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE };
    Cursor managedCursor;
	//HoursRestricted counter = new HoursRestricted(7, 21);
	HoursRestricted counter = new HoursRestricted(12, 21);

    private long getMeteredMinuteCount(Cursor cur) { 

        long meteredMinutesCount = 0;

        if (cur.moveToFirst()) {
            int dateColumn = cur.getColumnIndex(Calls.DATE); 
            int durationColumn = cur.getColumnIndex(Calls.DURATION);

            String phoneNumber; 

            do {
                long date, durationSeconds;

                date = cur.getLong(dateColumn);
                durationSeconds = cur.getLong(durationColumn);

                long meteredMinutes = counter.extractMeteredSeconds(date, durationSeconds) / 60;
				Log.d(TAG, "For duration " + Long.toString(durationSeconds) + " seconds, metered amount is " + Long.toString(meteredMinutes));

                meteredMinutesCount += meteredMinutes;

            } while (cur.moveToNext());

        } else {
            Log.d(TAG, "The provider is empty.  That's okay.");
        }

        return meteredMinutesCount;
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

    }


    /** Called when the activity is first created. */
    @Override
    public void onResume()
    {
		super.onResume();

        TextView v = (TextView) findViewById(R.id.countresult);
		if (v != null) {
			managedCursor = managedQuery(Calls.CONTENT_URI, projection, 
			//        Calls.TYPE + " <> " + Integer.toString(Calls.MISSED_TYPE),
			null,
					null, null);
			v.setText("metered minutes logged: " + Long.toString(getMeteredMinuteCount(managedCursor)));
		} else {
			Log.e(TAG, "view not found!");
			
		}
	}

}
