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

public class Tmobile extends HoursRestricted {

    public static final String TAG = "Tmobile";

    public Tmobile() {
    }


    @Override
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
        long ss = durationSeconds;
        long count;
        assert ss >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);  // sec to msec

        MeteredPeriodForDay period = new MeteredPeriodForDay(calCallStart);

        if (period.start == null) {
            //Log.d(TAG, "meter period start is null");
            count = 0;
        } else if (calCallStart.after(period.start) && calCallStart.before(period.end)) {
            count = ss;
        } else {
            //Log.d(TAG, "call is outside metered period");
            count = 0;
		}
        return count;
    }

    @Override
    @Test
    public void test_simple_metered() {
        Assert.assertEquals("zero", 0L, extractMeteredSeconds(monday_075026, 0L, "42", 42));
        Assert.assertEquals("one", 1L, extractMeteredSeconds(monday_075026, 1L, "42", 42));
        Assert.assertEquals("two", 2L, extractMeteredSeconds(monday_075026, 2L, "42", 42));
        Assert.assertEquals("lots", 300L, extractMeteredSeconds(monday_075026, 300L, "42", 42));
    }

    @Override
    @Test
    public void test_simple_weekend() {
        Assert.assertEquals("weekend no charge", 0L, extractMeteredSeconds(sunday_235026, 2L, "42", 42));
    }

    @Override
    @Test
    public void test_single_edges() {
        Assert.assertEquals("start during, cross out", 1500L, extractMeteredSeconds(monday_205026, 1500L, "42", 42));
        Assert.assertEquals("start before, cross into", 0L, extractMeteredSeconds(monday_065026, 1500L, "42", 42));
    }

    @Override
    @Test
    public void test_complex_weekday() {
        Assert.assertEquals("monday am to tuesday pm", 0L, extractMeteredSeconds(monday_065026, 172800, "42", 42));
        Assert.assertEquals("monday am to tuesday am", 86400, extractMeteredSeconds(monday_075026, 86400, "42", 42));
    }

    @Override
    @Test
    public void test_complex_weekend() {
        Assert.assertEquals("friday pm to monday am", 0L, extractMeteredSeconds(friday_235026, 194400, "42", 42));
    }

};


/* vim: set et sta ai: */
