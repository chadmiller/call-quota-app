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
        this.caller = getNormalizedNumber(caller);
        this.reasonForRate = reasonForRate;
    }

    public String getNormalizedNumber() {
        return getNormalizedNumber(this.caller);
    }

    public static String getNormalizedNumber(String source) {

        char[] dirtyChars = source.toCharArray();
        char[] cleanChars = new char[dirtyChars.length];
        
        int i = 0;
        for (char d: dirtyChars) {
            if (Character.isDigit(d)) {
                cleanChars[i] = d;
                i++;
            }
        }

        return new String(cleanChars).substring(0, i);
    }

}
/* vim: set et ai sta : */
