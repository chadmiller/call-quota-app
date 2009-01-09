package org.chad.jeejah.callquota.carrier;

import android.util.Log;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.util.GregorianCalendar;

import org.punit.*;
import org.punit.annotation.Test;
import org.punit.convention.AnnotationConvention;

import junit.framework.*;

public class HoursRestricted extends AllMeteredCeil {

    public static final String TAG = "HoursRestricted";

    public HoursRestricted() {
    }

    @Override
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
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

            totalSeconds += extractMeteredSecondsForDay(calCallStart, calCursor);

            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 0);
            calCursor.set(GregorianCalendar.MINUTE, 0);
            calCursor.set(GregorianCalendar.SECOND, 0);
            calCursor.set(GregorianCalendar.MILLISECOND, 0);

            calCursor.add(GregorianCalendar.DAY_OF_YEAR, (int) 1);
            calCallStart.setTimeInMillis(calCursor.getTimeInMillis());
        }

        totalSeconds += extractMeteredSecondsForDay(calCallStart, calCallEnd);

        Log.d(TAG, "extractMeteredSeconds(...)  -> " + Long.toString(totalSeconds));
        return totalSeconds;
    }

    protected class MeteredPeriodForDay {
        public GregorianCalendar start;
        public GregorianCalendar end;

        public MeteredPeriodForDay(GregorianCalendar t) {

            int dow = t.get(GregorianCalendar.DAY_OF_WEEK);

            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
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

        long count;

        MeteredPeriodForDay period = new MeteredPeriodForDay(calCallStart);

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


    protected final long  friday_235026 = 1230353426000L;
    protected final long  sunday_235026 = friday_235026 + (1000 * 24 * 60 * 60 * 2);
    protected final long  monday_065026 = sunday_235026 + (1000 *  7 * 60 * 60);
    protected final long  monday_075026 = monday_065026 + (1000 *  1 * 60 * 60);
    protected final long  monday_205026 = monday_075026 + (1000 * 13 * 60 * 60);
    protected final long tuesday_075026 = monday_075026 + (1000 * 24 * 60 * 60);

    //@Test(expected = NullPointerException.class)
    @Test
    public void test_simple_metered() {
        Assert.assertEquals("zero", 0L, extractMeteredSeconds(monday_075026, 0L, "42", 42));
        Assert.assertEquals("one", 1L, extractMeteredSeconds(monday_075026, 1L, "42", 42));
        Assert.assertEquals("two", 2L, extractMeteredSeconds(monday_075026, 2L, "42", 42));
        Assert.assertEquals("lots", 300L, extractMeteredSeconds(monday_075026, 300L, "42", 42));
    }

    @Test
    public void test_simple_weekend() {
        Assert.assertEquals("weekend no charge", 0L, extractMeteredSeconds(sunday_235026, 2L, "42", 42));
    }

    @Test
    public void test_single_edges() {
        Assert.assertEquals("start during, cross out", 600L-26L, extractMeteredSeconds(monday_205026, 1500L, "42", 42));
        Assert.assertEquals("start before, cross into", 1500L-(600L-26L), extractMeteredSeconds(monday_065026, 1500L, "42", 42));
    }

    @Test
    public void test_complex_weekday() {
        Assert.assertEquals("monday am to tuesday pm", 100800L, extractMeteredSeconds(monday_065026, 172800, "42", 42));
        Assert.assertEquals("monday am to tuesday am", 50400, extractMeteredSeconds(monday_075026, 86400, "42", 42));
    }

    @Test
    public void test_complex_weekend() {
        Assert.assertEquals("friday pm to monday am", 0L, extractMeteredSeconds(friday_235026, 194400, "42", 42));
    }

};


/* vim: set et sta ai: */
