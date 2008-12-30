package org.chad.jeejah.callquota.carrier;

public class AllMeteredCeil extends AllMetered {

    static final String TAG = "AllMeteredCeil";

    public AllMeteredCeil() {
		super();
	}

	@Override
    public long extractMeteredSeconds(long startTime, long durationSeconds) {
        long s = super.extractMeteredSeconds(startTime, durationSeconds);

        return (((long) Math.ceil(s/60.0)) * 60L);
    }

};

