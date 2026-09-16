package org.zstack.test.integration.identity

import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class IdentityTest extends Test {
    static SpringSpec springSpec = {
        def spec = makeSpring()

        spec.include("authentications/accountImport.xml")
        spec.include("authentications/accountImportTest.xml")
        spec.include("authentications/ldap.xml")

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

    static EnvSpec oneVmBasicEnv() {
        return SimpleTest.oneVmBasicEnv()
    }
}
