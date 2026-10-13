package org.zstack.test.integration.identity.account.freshinstall

import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountVO
import org.zstack.identity.AccountManagerImpl
import org.zstack.identity.AccountPassword
import org.zstack.identity.IdentityGlobalConfig
import org.zstack.identity.IdentityGlobalProperty
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AccountPasswordFreshInstallCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        // Also use the fresh bootstrap when this case is executed directly.
        boolean originalBootstrap = IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL
        onCleanExecute {
            IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = originalBootstrap
        }
        IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = true
        useSpring(makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        // No setStorage/updateGlobalConfig here: only real empty-database startup
        // may enable the persisted switch and encode the initial admin password.
        assert IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL
        assert IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.value(Boolean.class)
        assert Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, IdentityGlobalConfig.CATEGORY)
                .eq(GlobalConfigVO_.name, IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.name)
                .select(GlobalConfigVO_.value).findValue() == "true"

        def admin = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        String stored = admin.password
        def lastOpDate = admin.lastOpDate
        assert AccountPassword.isEncoded(stored)
        assert AccountPassword.matches(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD, stored)

        env.create {
            assert loginAsAdmin().accountUuid == admin.uuid
            [true, false].each { bootstrap ->
                // Reinitializing an existing database preserves the mode and admin,
                // including when an upgrade removes the local bootstrap hint.
                IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = bootstrap
                bean(AccountManagerImpl.class).prepareDbInitialValue()
                assert IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.value(Boolean.class)
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
