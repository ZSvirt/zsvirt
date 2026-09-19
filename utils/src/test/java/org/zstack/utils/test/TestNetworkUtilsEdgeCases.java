package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import static org.junit.Assert.assertEquals;

public class TestNetworkUtilsEdgeCases {
    @Test
    public void testConvertNetmaskBoundaries() {
        assertEquals("0.0.0.0", NetworkUtils.convertNetmask(0));
        assertEquals("128.0.0.0", NetworkUtils.convertNetmask(1));
        assertEquals("255.255.255.0", NetworkUtils.convertNetmask(24));
        assertEquals("255.255.255.255", NetworkUtils.convertNetmask(32));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertNetmaskRejectsInvalidPrefix() {
        NetworkUtils.convertNetmask(33);
    }
}
