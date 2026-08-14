package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.data.ArrayHelper;

import java.util.ArrayList;
import java.util.List;

public class TestArrayHelper {
    class Boy {
        String name;
        int age;
    }
    
    @Test
    public void test() {
        List<Boy> boys = new ArrayList<Boy>(10);
        for (int i=0; i<10; i++) {
            Boy b = new Boy();
            b.name = "Boy-" + i;
            b.age = i;
            boys.add(b);
        }
        String[] names = ArrayHelper.arrayFromField(boys, "name", String.class);
        for (int i=0; i<names.length; i++) {
            System.out.println(names[i]);
        }
    }
}
