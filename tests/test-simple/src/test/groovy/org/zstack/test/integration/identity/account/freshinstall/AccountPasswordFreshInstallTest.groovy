package org.zstack.test.integration.identity.account.freshinstall

import org.zstack.testlib.Test

class AccountPasswordFreshInstallTest extends Test {
    @Override
    protected void configProperty() {
        // The test must initialize a fresh database to exercise initial admin creation.
        assert DEPLOY_DB
    }

    @Override
    void setup() {
        useSpring(makeSpring())
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}