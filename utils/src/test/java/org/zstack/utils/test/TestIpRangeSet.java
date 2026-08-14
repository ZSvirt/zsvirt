package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.network.NetworkUtils;

import java.util.Set;

public class TestIpRangeSet {

    @Test
    public void test() {
        Set<String> ips = IpRangeSet.listAllIps("127.0.0.1-127.0.0.4, ^127.0.0.3", 100);
        assert ips.size() == 3;
        assert ips.contains("127.0.0.1");
        assert ips.contains("127.0.0.2");
        assert ips.contains("127.0.0.4");

        boolean meetException = false;
        try {
            IpRangeSet.listAllIps("127.0.0.0-127.0.8.1", 1000);
        } catch (Exception e) {
            meetException = true;
        }
        assert meetException;

        ips = IpRangeSet.listAllIps("0.0.0.0-255.255.255.255,^0.0.0.1-255.255.255.254", 2);
        assert ips.size() == 2;
        assert ips.contains("0.0.0.0");
        assert ips.contains("255.255.255.255");

        IpRangeSet set = IpRangeSet.generateIpRangeSet("192.168.1.1/32,^192.168.1.1/32,192.168.1.2/32");
        assert set.size() == 1;
        assert set.contains(NetworkUtils.ipv4StringToLong("192.168.1.2"));
    }
}
