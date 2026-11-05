package org.zstack.utils.network;

/**
 * Budget for GetFreeIp when one L3 network has several IP ranges.
 * Each range consumes only the addresses it returned. Subtracting the
 * accumulated total asks later ranges for fewer addresses than remain.
 */
public class FreeIpQueryLimit {
    private FreeIpQueryLimit() {
    }

    public static int next(int remaining, int fetchedFromThisRange) {
        if (remaining < 0 || fetchedFromThisRange < 0) {
            throw new IllegalArgumentException("free ip counts cannot be negative");
        }
        if (fetchedFromThisRange >= remaining) {
            return 0;
        }
        return remaining - fetchedFromThisRange;
    }
}
