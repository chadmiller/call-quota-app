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

    private String formatCalendar(GregorianCalendar c) {
        return String.format("   %04d-%02d-%02d  %02d:%02d:%02d", 
                c.get(GregorianCalendar.YEAR), 
                c.get(GregorianCalendar.MONTH)+1, 
                c.get(GregorianCalendar.DAY_OF_MONTH), 
                c.get(GregorianCalendar.HOUR_OF_DAY), 
                c.get(GregorianCalendar.MINUTE), 
                c.get(GregorianCalendar.SECOND));
    }

    @Override
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
        long ss = durationSeconds;
        assert ss >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);  // sec to msec

        //Log.d(TAG, String.format("extractMeteredSeconds(%d, %d)", startTimeInMs, durationSeconds));

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
        //Log.d(TAG, "  start: " + formatCalendar(calCallStart) + "   end: " + formatCalendar(calCallEnd) + "   for raw sec " + Long.toString(ss));

        if (period.start == null) {
            //Log.d(TAG, "  There is no metering on this day.   -> 0");
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
            //Log.d(TAG, "  Call begins after metering period ends.  ->  " + Long.toString(count));
        } else if (calCallEnd.before(period.start)) {
            count = 0; // a
            //Log.d(TAG, "  Call ends before metering period begins.  ->  " + Long.toString(count));
        } else {
            if (calCallEnd.after(period.end)) {
                if (calCallStart.before(period.start)) {
                    count = (period.end.getTimeInMillis() - period.start.getTimeInMillis()) / 1000; // f
                    //Log.d(TAG, "  Call spans a metered period, so only middle section counts.  ->  " + Long.toString(count));
                } else {
                    count = (period.end.getTimeInMillis() - calCallStart.getTimeInMillis()) / 1000; // d
                    //Log.d(TAG, "  Call begins metered but finished after period ends.  ->  " + Long.toString(count));
                }
            } else {  // == ends during metering
                if (calCallStart.before(period.start)) {
                    count = (calCallEnd.getTimeInMillis() - period.start.getTimeInMillis()) / 1000; // c
                    //Log.d(TAG, "  Call begins outside metering, but last part is metered.  ->  " + Long.toString(count));
                } else {
                    count = ss; // e
                    //Log.d(TAG, "  Call is in middle of metering period.  All of call matches.  ->  " + Long.toString(count));
                }
            }
        }

        return count;
    }


    private final long  friday_235026 = 1230353426000L;
    private final long  sunday_235026 = friday_235026 + (1000 * 24 * 60 * 60 * 2);
    private final long  monday_065026 = sunday_235026 + (1000 *  7 * 60 * 60);
    private final long  monday_075026 = monday_065026 + (1000 *  1 * 60 * 60);
    private final long  monday_205026 = monday_075026 + (1000 * 13 * 60 * 60);
    private final long tuesday_075026 = monday_075026 + (1000 * 24 * 60 * 60);

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
