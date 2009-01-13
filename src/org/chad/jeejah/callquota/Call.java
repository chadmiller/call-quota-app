package org.chad.jeejah.callquota;

import org.punit.*;
import org.punit.annotation.Test;
import org.punit.convention.AnnotationConvention;

import junit.framework.*;

public class Call {
	public final long beginningFromEpochSec;
	public final long endFromEpochSec;
	public final long meteredMinutes;
	public final String caller;

	Call(long beginningFromEpochSec, long endFromEpochSec, long meteredMinutes, String caller) {
		this.beginningFromEpochSec = beginningFromEpochSec;
		this.endFromEpochSec = endFromEpochSec;
		this.meteredMinutes = meteredMinutes;
		this.caller = getNormalizedNumber(caller);
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

		return new String(cleanChars);
	}

    @Test
    public void test_getNormalizedNumber() {
        Assert.assertEquals("1", "1", getNormalizedNumber("1"));
        Assert.assertEquals("2", "1", getNormalizedNumber("+1"));
        Assert.assertEquals("3", "123", getNormalizedNumber("+1-23"));
        Assert.assertEquals("4", "1234567", getNormalizedNumber("+1-23 4 5 67"));
        Assert.assertEquals("5", "1234567", getNormalizedNumber(" \000 + 1 -23 4 5 6   \n\rabc   7 "));
        Assert.assertEquals("6", "", getNormalizedNumber(""));
    }

}
