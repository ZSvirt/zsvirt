package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.IPv6NetworkUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestIPv6NetworkUtilsEdgeCases {
    @Test
    public void testRangeOverlap() {
        assertTrue(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::1", "2001:db8::10",
                "2001:db8::10", "2001:db8::20"));
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::1", "2001:db8::10",
                "2001:db8::11", "2001:db8::20"));
    }

    @Test
    public void testInvalidRangeDoesNotOverlap() {
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "2001:db8::10", "2001:db8::2",
                "2001:db8::1", "2001:db8::20"));
        assertFalse(IPv6NetworkUtils.isIpv6RangeOverlap(
                "not-an-ip", "2001:db8::2",
                "2001:db8::1", "2001:db8::20"));
    }
}
