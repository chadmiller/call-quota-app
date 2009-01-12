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

public class TmobileFaves extends Tmobile {

    public static final String TAG = "TmobileFaves";

    public TmobileFaves() {
    }

    

    @Override
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {

        return super.extractMeteredSeconds(startTimeInMs, durationSeconds, number, type);
    }
};


/* vim: set et sta ai: */
