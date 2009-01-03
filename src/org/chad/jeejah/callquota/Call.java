package org.chad.jeejah.callquota;

public class Call {
	public final long beginningFromEpochSec;
	public final long endFromEpochSec;
	public final long meteredMinutes;
	public final boolean firstAfterReset;

	Call(long beginningFromEpochSec, long endFromEpochSec, long meteredMinutes, boolean firstAfterReset) {
		this.beginningFromEpochSec = beginningFromEpochSec;
		this.endFromEpochSec = endFromEpochSec;
		this.meteredMinutes = meteredMinutes;
		this.firstAfterReset = firstAfterReset;
	}
}
