package org.zstack.test.integration.core

import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/3/2.
 */
class CoreLibraryTest extends Test {
    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("CloudBusAopProxy.xml")
            include("ZoneManager.xml")
            include("webhook.xml")
            include("log.xml")
        }
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
