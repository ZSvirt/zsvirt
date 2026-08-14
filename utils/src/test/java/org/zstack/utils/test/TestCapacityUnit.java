package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.data.SizeUnit;

public class TestCapacityUnit {
    @Test
    public void test() {
        System.out.println(SizeUnit.GIGABYTE.toByte(8));
        System.out.println(SizeUnit.BYTE.toGigaByte(8796093022208L));
        System.out.println(SizeUnit.TERABYTE.toByte(8));
        System.out.println(Long.MAX_VALUE);
    }
}
