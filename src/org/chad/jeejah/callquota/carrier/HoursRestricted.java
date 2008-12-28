package org.chad.jeejah.callquota.carrier;

import android.util.Log;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.util.GregorianCalendar;

public class HoursRestricted extends AllMeteredCeil {

    static final String TAG = "HoursRestricted";

    protected int startHour;
    protected int endHour;

    public HoursRestricted(int startHour, int endHour) {
        super();

        this.startHour = startHour;
        this.endHour = endHour;
    }

    @Override
    public long extractMeteredSeconds(long startTime, long durationSeconds) {
        long ss = super.extractMeteredSeconds(startTime, durationSeconds);
        assert ss >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setTimeInMillis(startTime * 1000);  // sec to msec

        GregorianCalendar calCallEnd = (GregorianCalendar) calCallStart.clone();
        calCallEnd.add(GregorianCalendar.SECOND, (int) ss);

        GregorianCalendar calCursor = (GregorianCalendar) calCallStart.clone();

        long totalSeconds = 0;
        while (calCursor.get(GregorianCalendar.DAY_OF_YEAR) < calCallEnd.get(GregorianCalendar.DAY_OF_YEAR)) {
            Log.d(TAG, "stepping through one day.            Hours     " + Integer.toString(calCursor.get(GregorianCalendar.HOUR_OF_DAY)) + " to hour " + Integer.toString(calCallEnd.get(GregorianCalendar.HOUR_OF_DAY)));

            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 23);
            calCursor.set(GregorianCalendar.MINUTE, 59);
            calCursor.set(GregorianCalendar.SECOND, 59);
            calCursor.set(GregorianCalendar.MILLISECOND, 999);

            totalSeconds += extractMeteredSecondsForDay(calCallStart, calCursor);

            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 0);
            calCursor.set(GregorianCalendar.MINUTE, 0);
            calCursor.set(GregorianCalendar.SECOND, 0);
            calCursor.set(GregorianCalendar.MILLISECOND, 0);

            calCursor.add(GregorianCalendar.DAY_OF_YEAR, (int) 1);
            calCallStart.setTimeInMillis(calCursor.getTimeInMillis());
        }

        totalSeconds += extractMeteredSecondsForDay(calCallStart, calCallEnd);

        return totalSeconds;
    }

    public class MeteredPeriodForDay {
        public GregorianCalendar start;
        public GregorianCalendar end;

        public MeteredPeriodForDay(GregorianCalendar t) {

            int dow = t.get(GregorianCalendar.DAY_OF_WEEK);

            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
                Log.d(TAG, "Weekends are free.");
                //  no period
            } else {
                start = (GregorianCalendar) t.clone();

                start.set(GregorianCalendar.HOUR_OF_DAY, 7);
                start.set(GregorianCalendar.MINUTE, 0);
                start.set(GregorianCalendar.SECOND, 0);
                start.set(GregorianCalendar.MILLISECOND, 0);
                
                end = (GregorianCalendar) start.clone();
                end.set(GregorianCalendar.HOUR_OF_DAY, 21);
            }
        }
    };

    protected long extractMeteredSecondsForDay(GregorianCalendar calCallStart, 
            GregorianCalendar calCallEnd) {

        long inside;
        long outside;
        MeteredPeriodForDay period = new MeteredPeriodForDay(calCallStart);

        if (period.start == null)
            return 0;

        if (period.end == null)
            return 0;

        long ss = (calCallEnd.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000;

        //  Cases:
        //   [         (=====================)         ]   [DAY] (METERED)
        // a   {---}
        // b                                    {---}
        // c   {------------}
        // d                            {---------}
        // e               {----------}
        // f     {---------------------------------}
        // g  ...--}                              {--...  // same as a or b
        // h  ...------------}                    {--...  // Ugh!
        // i  Loop.  Call is more than 24 hours.  The AT&T monthly-billing problem.
        // foo'  extra hard:  end before start.  Keep logic but use outside value;


        if (calCallEnd.before(period.start)) {
            Log.d(TAG, "a");
            outside = ss;
            inside = 0;
        } else if (calCallStart.after(period.end)) {
            Log.d(TAG, "b");
            outside = ss;
            inside = 0;
        } else if (calCallStart.after(period.start) && calCallEnd.before(period.end)) {
            Log.d(TAG, "e");
            inside = ss;
            outside = 0;
        } else if (calCallStart.before(period.start) && calCallEnd.before(period.end)) {
            Log.d(TAG, "c");
            inside = (calCallEnd.getTimeInMillis() - period.start.getTimeInMillis()) / 1000;
            outside = (period.start.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000;
        } else if (calCallStart.before(period.end) && calCallEnd.after(period.end)) {
            Log.d(TAG, "d");
            inside = (period.end.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000;
            outside = (calCallEnd.getTimeInMillis() - period.start.getTimeInMillis()) / 1000;
        } else if (calCallStart.before(period.start) && calCallEnd.after(period.end)) {
            Log.d(TAG, "f");
            inside = (period.end.getTimeInMillis() - period.start.getTimeInMillis()) / 1000;
            outside = 0;
            outside += (period.start.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000;
            outside += (calCallEnd.getTimeInMillis() - period.end.getTimeInMillis()) / 1000;
        } else {
            Log.e(TAG, "Unhandled time period:  call range " + Integer.toString(calCallStart.get(GregorianCalendar.HOUR_OF_DAY)) + ":" +Integer.toString(calCallStart.get(GregorianCalendar.MINUTE)) + " to " + Integer.toString(calCallEnd.get(GregorianCalendar.HOUR_OF_DAY)) + ":" +Integer.toString(calCallEnd.get(GregorianCalendar.MINUTE)));
            Log.e(TAG, "Unhandled time period: meter range " + Integer.toString(period.start.get(GregorianCalendar.HOUR_OF_DAY)) + ":" +Integer.toString(period.start.get(GregorianCalendar.MINUTE)) + " to " + Integer.toString(period.end.get(GregorianCalendar.HOUR_OF_DAY)) + ":" +Integer.toString(period.end.get(GregorianCalendar.MINUTE)));

            inside = ss; // FIXME  be paranoid.  overreport.
            outside = 0; // FIXME
        }

        return inside;
    }

};


/* vim: set et sta ai: */
