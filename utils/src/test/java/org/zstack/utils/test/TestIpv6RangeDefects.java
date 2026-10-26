package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.IPv6NetworkUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Invalid addresses must not be reported as overlap. A reversed numeric range
 * is not an overlap either. String order is not address order.
 */
public class TestIpv6RangeDefects {
    @Test
    public void unparsableRangeIsNotOverlap() {
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "not-an-ip", "also-not", "2001:db8::1", "2001:db8::2"));
    }

    @Test
    public void realOverlapIsStillDetected() {
        assertTrue(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::1", "2001:db8::10", "2001:db8::5", "2001:db8::20"));
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::1", "2001:db8::2", "2001:db8::10", "2001:db8::20"));
    }

    @Test
    public void reversedNumericRangeIsNotOverlap() {
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::10", "2001:db8::2", "2001:db8::1", "2001:db8::3"));
    }

    @Test
    public void rangeOrderFollowsAddressValueNotTextOrder() {
        assertTrue(IPv6NetworkUtils.isValidIpv6Range("2001:db8::2", "2001:db8::10"));
        assertFalse(IPv6NetworkUtils.isValidIpv6Range("2001:db8::10", "2001:db8::2"));
        assertFalse(IPv6NetworkUtils.isValidIpv6Range("not-an-ip", "2001:db8::1"));
    }
}
