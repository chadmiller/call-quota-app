package org.chad.jeejah.callquota;

import android.util.Log;
import java.util.HashSet;
import java.util.GregorianCalendar;
import android.provider.CallLog.Calls;

class Metering {
    public static final String TAG = "Metering";
    Configuration configuration;

    public Metering(Configuration configuration) {
        this.configuration = configuration;
    }


	/** Find the instant that ends a billing period. */
    public long getEndOfNthBillBackAsMs(int n, int billEndDayOfMonth) {
		Log.d(TAG, String.format("getEndOfNthBillBackAsMs: last day is %d", billEndDayOfMonth));
        GregorianCalendar billEnd = new GregorianCalendar();
		
		/* If the last day is between the start of the month and now, then the
		 * end is next month, so go there before rounding down. */
		if (billEndDayOfMonth < billEnd.get(GregorianCalendar.DAY_OF_MONTH)) {
			billEnd.add(GregorianCalendar.MONTH, 1);
		}
        billEnd.set(GregorianCalendar.DAY_OF_MONTH, billEndDayOfMonth);
		billEnd.add(GregorianCalendar.MONTH, (n * -1));
        billEnd.set(GregorianCalendar.HOUR, 0);
        billEnd.set(GregorianCalendar.MINUTE, 0);
        billEnd.set(GregorianCalendar.SECOND, 0);
        return billEnd.getTimeInMillis();
    }


    public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
        if (type == Calls.MISSED_TYPE)
            return 0;

        HashSet neverMeteredNormalized = this.configuration.getNumbersNeverMetered();
        if (neverMeteredNormalized != null) {
            if (neverMeteredNormalized.contains(Call.getNormalizedNumber(number))) {
                return 0;
            }
        }

        if (this.configuration.getMeteringStartsAtCallStart()) {
            return extractMeteredSecondsContiguousFromStart(startTimeInMs, durationSeconds);
        } else {
            return extractMeteredSecondsExactPeriod(startTimeInMs, durationSeconds);
        }
    
    }


    private long extractMeteredSecondsContiguousFromStart(long startTimeInMs, long durationSeconds) {
        long count;
        assert durationSeconds >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);  // sec to msec

        if (this.configuration.getMeteringOmitsWeekends()) {
            int dow = calCallStart.get(GregorianCalendar.DAY_OF_WEEK);
            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
                return 0;
            }
        }

        int startHour=0, endHour=24;
        if (this.configuration.getWantNightsFree()) {
            startHour = this.configuration.getDaytimeBeginningHour();
            endHour = this.configuration.getDaytimeEndHour();
        }

        MeteredPeriodForDay period = new MeteredPeriodForDay(calCallStart, startHour, endHour);

        assert (period.start==null) == (period.end==null);
        if (period.start == null) {
            count = 0;
        } else if (calCallStart.after(period.start) && calCallStart.before(period.end)) {
            count = durationSeconds;
        } else {
            count = 0;
        }
        return count;
    }


    protected class MeteredPeriodForDay {
        private static final String TAG = "MeteredPeriodForDay";

        public GregorianCalendar start;
        public GregorianCalendar end;

        public MeteredPeriodForDay(GregorianCalendar t, int startHour, int endHour) {
            if (endHour < startHour) {
                Log.e(TAG, "endHour is before startHour, and that makes no sense to me.  Yet.");
                return;
            }

            this.start = (GregorianCalendar) t.clone();

            this.start.setLenient(true);

            this.start.set(GregorianCalendar.HOUR_OF_DAY, startHour);
            this.start.set(GregorianCalendar.MINUTE, 0);
            this.start.set(GregorianCalendar.SECOND, 0);
            this.start.set(GregorianCalendar.MILLISECOND, 0);
            
            if ((startHour == 0) && (endHour == 24)) {
                // special case -- meter entire day.
                this.end = (GregorianCalendar) this.start.clone();
                this.end.set(GregorianCalendar.HOUR_OF_DAY, 23);
                this.end.set(GregorianCalendar.MINUTE, 59);
                this.end.set(GregorianCalendar.SECOND, 59);
                this.end.set(GregorianCalendar.MILLISECOND, 999);
            } else {
                this.end = (GregorianCalendar) this.start.clone();
                this.end.set(GregorianCalendar.HOUR_OF_DAY, endHour);
            }
        }
    };


    private long extractMeteredSecondsExactPeriod(long startTimeInMs, long durationSeconds) {
        long ss = durationSeconds;
        assert ss >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);  // sec to msec

        GregorianCalendar calCallEnd = (GregorianCalendar) calCallStart.clone();
        calCallEnd.add(GregorianCalendar.SECOND, (int) ss);

        GregorianCalendar calCursor = (GregorianCalendar) calCallStart.clone();

        long totalSeconds = 0;

        assert calCursor.compareTo(calCallEnd) <= 0;
        while (calCursor.get(GregorianCalendar.DAY_OF_YEAR) != calCallEnd.get(GregorianCalendar.DAY_OF_YEAR)) {
            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 23);
            calCursor.set(GregorianCalendar.MINUTE, 59);
            calCursor.set(GregorianCalendar.SECOND, 59);
            calCursor.set(GregorianCalendar.MILLISECOND, 999);

            totalSeconds += extractMeteredSecondsExactPeriodForDay(calCallStart, calCursor);

            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 0);
            calCursor.set(GregorianCalendar.MINUTE, 0);
            calCursor.set(GregorianCalendar.SECOND, 0);
            calCursor.set(GregorianCalendar.MILLISECOND, 0);

            calCursor.add(GregorianCalendar.DAY_OF_YEAR, (int) 1);
            calCallStart.setTimeInMillis(calCursor.getTimeInMillis());
        }

        totalSeconds += extractMeteredSecondsExactPeriodForDay(calCallStart, calCallEnd);

        Log.d(TAG, "extractMeteredSeconds(...)  -> " + Long.toString(totalSeconds));
        return totalSeconds;
    }


    protected long extractMeteredSecondsExactPeriodForDay(GregorianCalendar calCallStart, 
            GregorianCalendar calCallEnd) {

        if (this.configuration.getMeteringOmitsWeekends()) {
            int dow = calCallStart.get(GregorianCalendar.DAY_OF_WEEK);
            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
                return 0;
            }
        }

        int startHour=0, endHour=24;
        if (this.configuration.getWantNightsFree()) {
            startHour = this.configuration.getDaytimeBeginningHour();
            endHour = this.configuration.getDaytimeEndHour();
        }

        long count;

        MeteredPeriodForDay period = new MeteredPeriodForDay(calCallStart, startHour, endHour);

        assert (period.start == null) == (period.end == null);

        long ss = (calCallEnd.getTimeInMillis() / 1000) - (calCallStart.getTimeInMillis() / 1000);

        if (period.start == null) {
            return 0;
        }

        //  Cases:
        //   [         (=====================)         ]   [DAY] (METERED)
        // a   {---}
        // b                                    {---}
        // c   {------------}
        // d                            {---------}
        // e               {----------}
        // f     {---------------------------------}
        // i  Loop.  Call is more than 24 hours.  The AT&T monthly-billing problem.
        // x  Extra hard:  end before start.

        if (calCallStart.after(period.end)) {
            count = 0;  // b
        } else if (calCallEnd.before(period.start)) {
            count = 0; // a
        } else {
            if (calCallEnd.after(period.end)) {
                if (calCallStart.before(period.start)) {
                    count = (period.end.getTimeInMillis() - period.start.getTimeInMillis()) / 1000; // f
                } else {
                    count = (period.end.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000; // d
                }
            } else {  // == ends during metering
                if (calCallStart.before(period.start)) {
                    count = (calCallEnd.getTimeInMillis() - period.start.getTimeInMillis()) / 1000; // c
                } else {
                    count = ss; // e
                }
            }
        }

        return count;
    }



}
/* vim: set et ai sta : */
