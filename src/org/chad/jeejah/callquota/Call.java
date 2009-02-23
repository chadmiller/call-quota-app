package org.chad.jeejah.callquota;

public class Call {
    private static final String TAG = "CallQuota.Call";
    public final long beginningFromEpochMs;
    public final long endFromEpochMs;
    public final long meteredMinutes;
    public final String caller;
    public final String reasonForRate;

    Call(long beginningFromEpochMs, long endFromEpochMs, long meteredMinutes, String caller, String reasonForRate) {
        this.beginningFromEpochMs = beginningFromEpochMs;
        this.endFromEpochMs = endFromEpochMs;
        this.meteredMinutes = meteredMinutes;
        this.caller = caller;
        this.reasonForRate = reasonForRate;
    }

}
/* vim: set et ai sta : */
