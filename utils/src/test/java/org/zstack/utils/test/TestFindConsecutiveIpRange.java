package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.data.Pair;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class TestFindConsecutiveIpRange {

    @Test
    public void test() {
        List<String> ips = new ArrayList<String>();
        
        ips.add("192.168.0.10");
        ips.add("192.167.0.10");
        ips.add("192.168.0.13");
        ips.add("192.168.0.14");
        ips.add("192.168.0.15");
        ips.add("192.168.1.100");
        ips.add("192.167.0.80");
        ips.add("192.167.0.81");
        ips.add("192.167.0.83");
        
        List<Pair<String, String>> ret = NetworkUtils.findConsecutiveIpRange(ips);
        for (Pair<String, String> p : ret) {
            System.out.println(p);
        }
        
    }
}
