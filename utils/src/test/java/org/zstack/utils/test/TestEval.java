package org.zstack.utils.test;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.Assert;
import org.junit.Test;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/11/2015.
 */
public class TestEval {
    public static class A {
    }

    @Test
    public void test() throws IOException {
        List<String> lst = list("CloudBus.serverIp.1", "CloudBus.serverIp.5", "CloudBus.serverIp.2", "CloudBus.serverIp.0");
        Collections.sort(lst);
        System.out.println(lst);

        System.out.println(NetworkUtils.isNetmask("255.255.255.0"));
        System.out.println(NetworkUtils.isNetmask("255.255.255.088"));
        System.out.println(NetworkUtils.isNetmaskExcept("0.0.0.0", "0.0.0.0"));

        SubnetUtils sub = new SubnetUtils("192.168.0.10/16");
        System.out.println(String.format("11111 %s", sub.getInfo().isInRange("192.168.0.1")));

        sub = new SubnetUtils("192.168.55.10/24");
        System.out.println(String.format("22222 %s", sub.getInfo().isInRange("192.168.0.1")));

        A a1 = new A();
        A a2 = new A();
        A a3 = new A();
        System.out.println(a1.getClass().hashCode());
        System.out.println(a2.getClass().hashCode());
        System.out.println(a3.getClass().hashCode());
        Assert.assertTrue(a1.getClass() == a2.getClass());
        Assert.assertTrue(a2.getClass() == a3.getClass());

        long time = SizeUnit.GIGABYTE.convert(19862528, SizeUnit.BYTE);
        System.out.println(String.format("time:%s", time));
        long during = TimeUnit.HOURS.convert(17, TimeUnit.MILLISECONDS);
        System.out.println(String.format("during:%s", during));
        double mem = time * during * 1.0;
        System.out.println(String.format("total:%s", mem));

        sub = new SubnetUtils("224.0.0.0/24");
        System.out.println(sub.getInfo().getLowAddress());
        System.out.println(sub.getInfo().getHighAddress());
        System.out.println(sub.getInfo().getCidrSignature());

        sub = new SubnetUtils("224.0.1.1", "255.255.255.0");
        System.out.println(sub.getInfo().getCidrSignature());

        sub = new SubnetUtils(sub.getInfo().getNetworkAddress(), "255.255.255.0");
        System.out.println(sub.getInfo().getCidrSignature());

        Runnable r = () -> {
            System.out.println("test lambda");
            System.out.println("test lambda2");
        };

        r.run();

        String testJson = "[\"files/kvm/libvirtd.conf\",\"/etc/libvirt/libvirtd.conf\"]";
        List collection = JSONObjectUtil.toCollection(testJson, ArrayList.class, String.class);
        System.out.println(collection);

        Assert.assertTrue(NetworkUtils.isCidrOverlap("192.168.0.1/24", "192.168.1.0/16"));

        Assert.assertTrue(NetworkUtils.isIpv4InCidr("192.168.201.100", "192.168.200.0/16"));
        Assert.assertFalse(NetworkUtils.isIpv4InCidr("192.168.201.100", "192.168.200.0/24"));


    }
}
