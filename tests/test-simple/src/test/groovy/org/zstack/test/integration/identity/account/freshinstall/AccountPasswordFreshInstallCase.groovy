package org.zstack.test.integration.identity.account.freshinstall

import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountVO
import org.zstack.identity.AccountManagerImpl
import org.zstack.identity.AccountPassword
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AccountPasswordFreshInstallCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        // Initial admin creation must use salted storage without any opt-in setting.
        def admin = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        String stored = admin.password
        def lastOpDate = admin.lastOpDate
        assert AccountPassword.isEncoded(stored)
        assert AccountPassword.matches(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD, stored)

        env.create {
            assert loginAsAdmin().accountUuid == admin.uuid
            2.times {
                bean(AccountManagerImpl.class).prepareDbInitialValue()
                def unchanged = dbFindByUuid(admin.uuid, AccountVO.class)
                assert unchanged.password == stored
                assert unchanged.lastOpDate == lastOpDate
                assert loginAsAdmin().accountUuid == admin.uuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}