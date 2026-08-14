package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class TestFindRandomIp {
    private List<Long> populateUsedIps(long s1, long e1, int skip) {
        List<Long> lst1 = new ArrayList<Long>((int) (e1 - s1 + 1));
        for (long i=s1; i< e1 - 11 + 1; i ++) {
            if (i % skip == 0) {
                continue;
            }
            lst1.add(i);
        }
        return lst1;
    }
    
	@Test
	public void test() {
        long s1 = NetworkUtils.ipv4StringToLong("10.0.0.2");
        long e1 = NetworkUtils.ipv4StringToLong("10.255.255.255");
        List<Long> al = populateUsedIps(s1, e1, 1000);
        long s = System.currentTimeMillis();
        String ret = NetworkUtils.randomAllocateIpv4Address(NetworkUtils.longToIpv4String(s1), NetworkUtils.longToIpv4String(e1), al);
        long e = System.currentTimeMillis();
        System.out.println(ret);
        System.out.println(e-s);
	}

}
