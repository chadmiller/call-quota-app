package org.chad.jeejah.callquota.carrier;

import android.util.Log;
import java.util.GregorianCalendar;

public class AllMetered {

	static final String TAG = "AllMetered";

	private int billEndDayOfMonth = 6;

    public AllMetered() {
	}

    public long getEndOfNthBillBackAsMs(int n) {
        GregorianCalendar billEnd = new GregorianCalendar();
        billEnd.set(GregorianCalendar.DAY_OF_MONTH, billEndDayOfMonth);
        billEnd.add(GregorianCalendar.MONTH, (n * -1));
        billEnd.set(GregorianCalendar.HOUR, 0);
        billEnd.set(GregorianCalendar.MINUTE, 0);
        billEnd.set(GregorianCalendar.SECOND, 0);
        return billEnd.getTimeInMillis();
    }

	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
		return durationSeconds;
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

};

