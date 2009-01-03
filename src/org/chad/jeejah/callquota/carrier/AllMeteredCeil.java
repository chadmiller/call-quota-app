package org.chad.jeejah.callquota.carrier;

import org.chad.jeejah.callquota.Configuration;

public class AllMeteredCeil extends AllMetered {
    static final String TAG = "AllMeteredCeil";

    public AllMeteredCeil() {
		super();
	}

	@Override
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
        long s = super.extractMeteredSeconds(startTimeInMs, durationSeconds, number, type);

        return (((long) Math.ceil(s/60.0)) * 60L);
    }

};

