package org.chad.jeejah.callquota.carrier;

public class AllMetered {

	static final String TAG = "AllMetered";

    public AllMetered() {
	}

	public long getPeriodStart() {
		return 1229094000000L;
	}

	public long getPeriodEnd() {
		return 1230822000000L; // >>> datetime.datetime(2009, 1, 1, 10, 0, 0, 0).strftime("%s")   FIXME
	}

	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type) {
		return durationSeconds;
	}

};

