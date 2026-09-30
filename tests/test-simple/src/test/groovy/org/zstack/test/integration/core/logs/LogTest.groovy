package org.zstack.test.integration.core.logs

import org.zstack.testlib.Test

class LogTest extends Test {
    @Override
    void setup() {
        def spec = makeSpring()
        spec.include("LogConfiguration.xml")
        spec.include("Log4j2Configuration.xml")
        useSpring(spec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
