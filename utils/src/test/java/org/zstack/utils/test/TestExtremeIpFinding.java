package org.zstack.utils.test;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

public class TestExtremeIpFinding {
    private String startIp = "10.0.0.0";
    private String gapIp = "10.255.255.253";
    private String endIp = "10.255.255.255";

    @Before
    public void setUp() throws Exception {
        long s = NetworkUtils.ipv4StringToLong(startIp);
        long g = NetworkUtils.ipv4StringToLong(gapIp);
        Long[] ips = new Long[(int) (g-s+2)];
        for (long i=s; i<g+1; i++) {
            ips[(int) (i-s)] = i;
        }
        ips[ips.length-1] = NetworkUtils.ipv4StringToLong(endIp);
        
        long st = System.currentTimeMillis();
        String ip = NetworkUtils.findFirstAvailableIpv4Address(startIp, endIp, ips);
        long et = System.currentTimeMillis();
        System.out.println(String.format("Finding ip[%s] using %s ms", ip, et - st));
        Assert.assertEquals("10.255.255.254", ip);
    }

    @Test
    public void test() {
    }

}
