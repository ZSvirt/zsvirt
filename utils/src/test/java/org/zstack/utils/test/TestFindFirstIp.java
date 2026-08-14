package org.zstack.utils.test;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class TestFindFirstIp {
    String sip = "10.0.110.2";
    String eip = "10.0.255.255";

    @Before
    public void setUp() throws Exception {
    }

    private Long[] populateUsedIps(long s1, long e1, int skip) {
        List<Long> lst1 = new ArrayList<Long>((int) (e1 - s1 + 1));
        for (long i=s1; i< e1 - 11 + 1; i ++) {
            if (i % skip == 0) {
                continue;
            }
            lst1.add(i);
        }
        Long[] a1 = lst1.toArray(new Long[lst1.size()]);
        return a1;
    }
    
    private Long[] ipsToLongArray(String...ips) {
        Long[] ret = new Long[ips.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = NetworkUtils.ipv4StringToLong(ips[i]);
        }
        return ret;
    }
    
    @Test
    public void test() {
        long lsip = NetworkUtils.ipv4StringToLong(sip);
        long leip = NetworkUtils.ipv4StringToLong(eip);
        
        long s1 = NetworkUtils.ipv4StringToLong("10.0.110.2");
        long e1 = NetworkUtils.ipv4StringToLong("10.0.255.100");
        Long[] a1 = populateUsedIps(s1, e1, 3);
        long ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        String ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.3", ip);
        
        s1 = NetworkUtils.ipv4StringToLong("10.0.110.5");
        e1 = NetworkUtils.ipv4StringToLong("10.0.110.100");
        a1 = populateUsedIps(s1, e1, 3);
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.2", ip);
        
        s1 = NetworkUtils.ipv4StringToLong("10.0.110.2");
        e1 = NetworkUtils.ipv4StringToLong("10.0.110.100");
        a1 = populateUsedIps(s1, e1, 10);
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.10", ip);
        
        a1 = ipsToLongArray("10.0.110.2", "10.0.110.4", "10.0.110.8");
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.3", ip);
        
        a1 = ipsToLongArray("10.0.110.2", "10.0.110.4", "10.0.110.5");
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.3", ip);
        
        a1 = ipsToLongArray("10.0.110.2");
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.3", ip);
        
        a1 = ipsToLongArray("10.0.110.2", "10.0.110.3");
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.4", ip);
        
        a1 = new Long[0];
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.2", ip);
        
        a1 = ipsToLongArray("10.0.110.2", "10.0.110.3", "10.0.110.4", "10.0.110.5", "10.0.110.7", "10.0.110.9", "10.0.110.10");
        ret = NetworkUtils.findFirstAvailableIpv4Address(lsip, leip, a1);
        ip = NetworkUtils.longToIpv4String(ret);
        Assert.assertEquals("10.0.110.6", ip);
        
        int arr[] = {1, 2, 3, 4, 5, 6, 8};
        int r = 0;
        for (int i=0; i<arr.length; i++) {
            r ^= arr[i]; 
        }
        System.out.println(r);
    }
}
