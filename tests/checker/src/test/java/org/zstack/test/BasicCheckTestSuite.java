package org.zstack.test;

import org.junit.Test;
import org.zstack.test.checker.JUnitCheckTestSuite;

public class BasicCheckTestSuite {
    @Test
    public void test() {
        JUnitCheckTestSuite.runAllTestCases();
    }
}
