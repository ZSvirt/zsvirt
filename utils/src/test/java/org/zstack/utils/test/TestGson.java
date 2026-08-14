package org.zstack.utils.test;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestGson {
    CLogger logger = Utils.getLogger(TestGson.class);

    public static class B {
        String address;
    }
    
    public static class A {
        String name;
        List<B> bs = new ArrayList<B>(3);
    }

    public static class C extends HashMap {
        String name;
    }
    
    @Test
    public void test() {
        B b = new B();
        b.address = "home";
        A a = new A();
        a.name = "hello";
        a.bs.add(b);
        a.bs.add(b);
        a.bs.add(b);
        
        System.out.println(JSONObjectUtil.toJsonString(a));

        b = JSONObjectUtil.toObject("{\"address\": null }", B.class);
        Assert.assertNull(b.address);

        C c = new C();
        c.name = "c";

        logger.debug(JSONObjectUtil.toJsonString(c));
    }
}
