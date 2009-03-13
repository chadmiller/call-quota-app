package org.chad.jeejah.callquota;

import android.util.Log;
import java.util.Set;
import java.util.GregorianCalendar;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;

class Metering {
    public static final String TAG = "CallQuota.Metering";
    Configuration configuration;

    private class CountAndReason {
        public long countMs;
        public String reason;
        public CountAndReason(long countMs, String reason) {
            this.countMs = countMs;
            this.reason = reason;
        }
    }

    public Metering(Configuration configuration) {
        this.configuration = configuration;
    }


    /** Find the instant that ends a billing period. */
    public long getEndOfNthBillBackAsMs(int n, int billEndDayOfMonth) {
        GregorianCalendar billEnd = new GregorianCalendar();

        Log.d(TAG, "time zone for calendar is " + billEnd.getTimeZone().getDisplayName());
        
        /* If the last day is between the start of the month and now, then the
         * end is next month, so go there before rounding down. */
        if (billEndDayOfMonth < billEnd.get(GregorianCalendar.DAY_OF_MONTH)) {
            billEnd.add(GregorianCalendar.MONTH, 1);
        }
        billEnd.add(GregorianCalendar.MONTH, (n * -1));
        billEnd.set(GregorianCalendar.DAY_OF_MONTH, billEndDayOfMonth);
        billEnd.set(GregorianCalendar.HOUR_OF_DAY, billEnd.getActualMaximum(GregorianCalendar.HOUR_OF_DAY));
        billEnd.set(GregorianCalendar.MINUTE, billEnd.getActualMaximum(GregorianCalendar.MINUTE));
        billEnd.set(GregorianCalendar.SECOND, billEnd.getActualMaximum(GregorianCalendar.SECOND));
        billEnd.set(GregorianCalendar.MILLISECOND, billEnd.getActualMaximum(GregorianCalendar.MILLISECOND));
        return billEnd.getTimeInMillis();
    }


    public Call recordCallInfo(long startTimeInMs, long durationMs, String number, int type) {
        CountAndReason cr = extractMeteredMs(startTimeInMs, durationMs, number, type);
        return new Call(startTimeInMs, startTimeInMs+durationMs, (long) Math.ceil(cr.countMs/60000.0), number, cr.reason);
    }


    private CountAndReason extractMeteredMs(long startTimeInMs, long durationMs, String number, int type) {
        if (type == Calls.MISSED_TYPE)
            return new CountAndReason(0, "unanswered");

        if (type == Calls.INCOMING_TYPE)
            if (! this.configuration.getIncomingCallsAreMetered())
                return new CountAndReason(0, "incoming");

        Set neverMeteredNormalized = this.configuration.getNumbersNeverMetered();
        if (neverMeteredNormalized != null) {
            if (neverMeteredNormalized.contains(PhoneNumberUtils.toCallerIDMinMatch(number))) {
                return new CountAndReason(0, "free friend");
            }
        }

        if (this.configuration.getMeteringStartsAtCallStart()) {
            return extractMeteredMsContiguousFromStart(startTimeInMs, durationMs);
        } else {
            return extractMeteredMsExactPeriod(startTimeInMs, durationMs);
        }
    
    }


    private CountAndReason extractMeteredMsContiguousFromStart(long startTimeInMs, long durationMs) {
        long count;
        assert durationMs >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);

        if (this.configuration.getMeteringOmitsWeekends()) {
            int dow = calCallStart.get(GregorianCalendar.DAY_OF_WEEK);
            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
                return new CountAndReason(0, "weekend");
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
            return new CountAndReason(0, "UNKNOWN"); // FIXME
        } else if (calCallStart.after(period.start) && calCallStart.before(period.end)) {
            return new CountAndReason(durationMs, "metered!");
        } else {
            return new CountAndReason(0, "nighttime");
        }
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


    private CountAndReason extractMeteredMsExactPeriod(long startTimeInMs, long durationMs) {
        long ss = durationMs;
        assert ss >= 0;

        GregorianCalendar calCallStart = new GregorianCalendar();
        calCallStart.setLenient(false);
        calCallStart.setTimeInMillis(startTimeInMs);

        GregorianCalendar calCallEnd = (GregorianCalendar) calCallStart.clone();
        calCallEnd.add(GregorianCalendar.MILLISECOND, (int) ss);

        GregorianCalendar calCursor = (GregorianCalendar) calCallStart.clone();

        long totalMs = 0;

        assert calCursor.compareTo(calCallEnd) <= 0;
        while (calCursor.get(GregorianCalendar.DAY_OF_YEAR) != calCallEnd.get(GregorianCalendar.DAY_OF_YEAR)) {
            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 23);
            calCursor.set(GregorianCalendar.MINUTE, 59);
            calCursor.set(GregorianCalendar.SECOND, 59);
            calCursor.set(GregorianCalendar.MILLISECOND, 999);

            totalMs += extractMeteredMsExactPeriodForDay(calCallStart, calCursor).countMs;

            calCursor.set(GregorianCalendar.HOUR_OF_DAY, 0);
            calCursor.set(GregorianCalendar.MINUTE, 0);
            calCursor.set(GregorianCalendar.SECOND, 0);
            calCursor.set(GregorianCalendar.MILLISECOND, 0);

            calCursor.add(GregorianCalendar.DAY_OF_YEAR, (int) 1);
            calCallStart.setTimeInMillis(calCursor.getTimeInMillis());
        }

        totalMs += extractMeteredMsExactPeriodForDay(calCallStart, calCallEnd).countMs;

        return new CountAndReason(totalMs, "mixed");
    }


    private CountAndReason extractMeteredMsExactPeriodForDay(GregorianCalendar calCallStart, 
            GregorianCalendar calCallEnd) {

        if (this.configuration.getMeteringOmitsWeekends()) {
            int dow = calCallStart.get(GregorianCalendar.DAY_OF_WEEK);
            if ((dow < GregorianCalendar.MONDAY) || (dow > GregorianCalendar.FRIDAY)) {
                return new CountAndReason(0, "weekend");
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

        long ss = calCallEnd.getTimeInMillis() - calCallStart.getTimeInMillis();

        if (period.start == null) {
            return new CountAndReason(0, "UNKNOWN");  // FIXME
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
                    count = period.end.getTimeInMillis() - period.start.getTimeInMillis(); // f
                } else {
                    count = period.end.getTimeInMillis() - calCallStart.getTimeInMillis(); // d
                }
            } else {  // == ends during metering
                if (calCallStart.before(period.start)) {
                    count = calCallEnd.getTimeInMillis() - period.start.getTimeInMillis(); // c
                } else {
                    count = ss; // e
                }
            }
        }

        return new CountAndReason(count, "mixture");  // FIXME
    }



}
/* vim: set et ai sta : */
