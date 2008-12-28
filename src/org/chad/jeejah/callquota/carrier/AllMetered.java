package org.chad.jeejah.callquota.carrier;

public class AllMetered {

	static final String TAG = "AllMetered";

    public AllMetered() {
	}

	public long extractMeteredSeconds(long startTime, long durationSeconds) {
		return durationSeconds;
	}

};

