package org.zstack.utils.test;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestURI {

    @Test
    public void test() throws URISyntaxException, UnsupportedEncodingException, MalformedURLException {
        String password = "zstackqwe:!@#";
        String url = String.format("root:%s@172.16.36.184/", password);
        int at = url.lastIndexOf("@");
        String userinfo = url.substring(0, at);
        System.out.println(userinfo);
        String rest = url.substring(at + 1, url.length());
        System.out.println(rest);
        System.out.println(userinfo.split(":")[0]);

        List<Integer> nums = list(1, 2, 3);
        nums.add(1, 5);
        System.out.println(nums);

        long v = 20580050944L;
        double val = (double) v / 2.5;
        System.out.println(Math.round(v / 2.5));
        System.out.println(String.valueOf((long)val));
    }
}
