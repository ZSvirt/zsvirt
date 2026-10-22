package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.FreeIpQueryLimit;

import static org.junit.Assert.assertEquals;

/**
 * GetFreeIp walks several ranges. The next range may only be asked for the
 * addresses still missing. Subtracting the accumulated total asks later
 * ranges for fewer addresses than the caller requested.
 */
public class TestFreeIpQueryLimit {
    @Test
    public void eachRangeConsumesOnlyWhatItReturned() {
        int requested = 10;
        int afterFirstRange = FreeIpQueryLimit.next(requested, 4);
        assertEquals(6, afterFirstRange);

        int afterSecondRange = FreeIpQueryLimit.next(afterFirstRange, 3);
        assertEquals(3, afterSecondRange);
    }

    @Test
    public void emptyRangeDoesNotShrinkTheBudget() {
        assertEquals(6, FreeIpQueryLimit.next(6, 0));
    }
}
