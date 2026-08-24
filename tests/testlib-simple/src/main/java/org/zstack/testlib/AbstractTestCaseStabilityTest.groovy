package org.zstack.testlib

import org.zstack.core.StartMode

abstract class AbstractTestCaseStabilityTest extends Test {
    abstract SpringSpec getSpringSpec()

    @Override
    void setup() {
        useSpring(getSpringSpec())
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }

    @Override
    StartMode getCaseMode() {
        return getStabilityTestStartMode()
    }
}
