package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.CollectionUtils;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestCollectionsUtils {

    public static class TestClass {
        String key;

        TestClass(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Test
    public void test() {
        String keySeed = "asdfghjklqwertyuiopzxcvbnm";
        List<TestClass> tests1 = list(new TestClass("aaaaa"), new TestClass("bbbbb"), new TestClass("ccccc"));
        CollectionUtils.shuffleByKeySeed(tests1, keySeed, TestClass::getKey);

        List<TestClass> tests2 = list(new TestClass("aaaaa"), new TestClass("bbbbb"), new TestClass("ccccc"));
        CollectionUtils.shuffleByKeySeed(tests2, keySeed, TestClass::getKey);

        List<TestClass> tests3 = list(new TestClass("aaaaa"), new TestClass("bbbbb"), new TestClass("ccccc"));
        CollectionUtils.shuffleByKeySeed(tests3, keySeed, TestClass::getKey);

        for (int i = 0; i < 2; i++) {
            assert tests1.get(i).key.equals(tests2.get(i).key);
            assert tests1.get(i).key.equals(tests3.get(i).key);
        }
    }
}
