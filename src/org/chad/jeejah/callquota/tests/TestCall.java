package org.chad.jeejah.callquota;

import org.punit.*;
import org.punit.annotation.Test;
import org.punit.convention.AnnotationConvention;

import junit.framework.*;

public class TestCall {

    @Test
    public void test_getNormalizedNumber() {
        Assert.assertEquals("1", "1", Call.getNormalizedNumber("1"));
        Assert.assertEquals("2", "1", Call.getNormalizedNumber("+1"));
        Assert.assertEquals("3", "123", Call.getNormalizedNumber("+1-23"));
        Assert.assertEquals("4", "1234567", Call.getNormalizedNumber("+1-23 4 5 67"));
        Assert.assertEquals("5", "1234567", Call.getNormalizedNumber(" \000 + 1 -23 4 5 6   \n\rabc   7 "));
        Assert.assertEquals("6", "", Call.getNormalizedNumber(""));
    }

}
/* vim: set et ai sta : */
