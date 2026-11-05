package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import static org.junit.Assert.assertEquals;

/**
 * A prefix of 0 is a real netmask. The shift in convertNetmask masks the
 * distance to 5 bits, so a distance of 32 used to stay 255.255.255.255.
 */
public class TestConvertNetmask {
    @Test
    public void prefixZeroIsAllZeroBits() {
        assertEquals("0.0.0.0", NetworkUtils.convertNetmask(0));
    }

    @Test
    public void ordinaryPrefixesKeepTheirHighBits() {
        assertEquals("128.0.0.0", NetworkUtils.convertNetmask(1));
        assertEquals("255.255.255.0", NetworkUtils.convertNetmask(24));
        assertEquals("255.255.255.255", NetworkUtils.convertNetmask(32));
    }

    @Test(expected = IllegalArgumentException.class)
    public void prefixPast32IsRejected() {
        NetworkUtils.convertNetmask(33);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePrefixIsRejected() {
        NetworkUtils.convertNetmask(-1);
    }
}
