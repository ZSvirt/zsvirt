package org.zstack.test.integration.telemetry

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class TelemetryTest extends Test {
    static SpringSpec springSpec = {
        def spec = makeSpring()
        spec.include("telemetry.xml")
        return spec
    }()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
