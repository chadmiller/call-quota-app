package org.chad.jeejah.callquota.carrier;

interface Carrier {
	String TAG = "Carrier";
	public long extractMeteredSeconds(long startTimeInMs, long durationSeconds, String number, int type);
}
