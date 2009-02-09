package org.chad.jeejah.callquota;

public class Call {
    private static final String TAG = "CallQuota.Call";
    public final long beginningFromEpochSec;
    public final long endFromEpochSec;
    public final long meteredMinutes;
    public final String caller;
    public final String reasonForRate;

    Call(long beginningFromEpochSec, long endFromEpochSec, long meteredMinutes, String caller, String reasonForRate) {
        this.beginningFromEpochSec = beginningFromEpochSec;
        this.endFromEpochSec = endFromEpochSec;
        this.meteredMinutes = meteredMinutes;
        this.caller = caller;
        this.reasonForRate = reasonForRate;
    }

}
/* vim: set et ai sta : */
