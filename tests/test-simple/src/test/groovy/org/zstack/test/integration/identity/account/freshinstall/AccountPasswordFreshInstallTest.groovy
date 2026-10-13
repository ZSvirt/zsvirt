package org.zstack.test.integration.identity.account.freshinstall

import org.junit.After
import org.zstack.identity.IdentityGlobalProperty
import org.zstack.testlib.Test

class AccountPasswordFreshInstallTest extends Test {
    boolean originalBootstrap

    @Override
    protected void configProperty() {
        originalBootstrap = IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL
        // Platform links global properties before this hook; set the installer hint
        // before Test redeploys the database and starts the real Spring components.
        IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = true
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

    @After
    void restoreBootstrap() {
        IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = originalBootstrap
    }
}
