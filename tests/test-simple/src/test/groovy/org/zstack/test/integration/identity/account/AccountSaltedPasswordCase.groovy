package org.zstack.test.integration.identity.account

import org.zstack.core.db.Q
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountVO
import org.zstack.header.identity.AccountVO_
import org.zstack.identity.AccountLoginBackend
import org.zstack.identity.AccountManagerImpl
import org.zstack.identity.IdentityGlobalProperty
import org.zstack.identity.AccountPassword
import org.zstack.identity.IdentityGlobalConfig
import org.zstack.loginControl.PasswordStrategyGlobalConfig
import org.zstack.loginControl.entity.HistoricalPasswordVO
import org.zstack.loginControl.entity.HistoricalPasswordVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.twoFactorAuthentication.TwoFactorAuthenticationAccountFactory
import org.zstack.twoFactorAuthentication.TwoFactorAuthenticationParamStruct
import org.apache.commons.codec.digest.DigestUtils

class AccountSaltedPasswordCase extends SubCase {
    EnvSpec env
    String originalConfig
    String originalHistoryConfig

    @Override
    void setup() {
        def spec = makeSpring()
        spec.xmls.addAll(IdentityTest.springSpec.xmls)
        spec.include("LoginControl.xml")
        spec.include("TwoFactorAuthentication.xml")
        useSpring(spec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    void setStorage(boolean enabled) {
        updateGlobalConfig {
            category = IdentityGlobalConfig.CATEGORY
            name = IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.name
            value = enabled.toString()
        }
    }

    @Override
    void test() {
        originalConfig = IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.value()
        originalHistoryConfig = PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.value()
        env.create {
            SessionInventory admin = loginAsAdmin()
            setStorage(false)
            def existingAdmin = dbFindByUuid(admin.accountUuid, AccountVO.class)
            String existingAdminPassword = existingAdmin.password
            def existingAdminLastOpDate = existingAdmin.lastOpDate
            boolean originalBootstrap = IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL
            try {
                // Installing another node against an old database must not enable salted writes.
                IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = true
                bean(AccountManagerImpl.class).prepareDbInitialValue()
                assert !IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.value(Boolean.class)
                def unchangedAdmin = dbFindByUuid(admin.accountUuid, AccountVO.class)
                assert unchangedAdmin.password == existingAdminPassword
                assert unchangedAdmin.lastOpDate == existingAdminLastOpDate
            } finally {
                IdentityGlobalProperty.SALTED_PASSWORD_ON_FRESH_INSTALL = originalBootstrap
            }
            String oldInput = DigestUtils.sha512Hex("legacy-password")
            String newInput = DigestUtils.sha512Hex("new-password")
            AccountInventory legacy = createAccount {
                name = "legacy-storage"
                password = oldInput
            } as AccountInventory
            assert dbFindByUuid(legacy.uuid, AccountVO.class).password == oldInput

            // Legacy login keeps the database's existing comparison semantics.
            boolean oldDatabaseMatch = Q.New(AccountVO.class).eq(AccountVO_.uuid, legacy.uuid)
                    .eq(AccountVO_.password, oldInput.toUpperCase()).isExists()
            assert bean(AccountLoginBackend.class).authenticate(legacy.name, oldInput.toUpperCase()) == oldDatabaseMatch

            setStorage(true)
            SessionInventory session = logInByAccount {
                accountName = legacy.name
                password = oldInput
            } as SessionInventory
            assert session.accountUuid == legacy.uuid
            assert dbFindByUuid(legacy.uuid, AccountVO.class).password == oldInput

            AccountInventory salted = createAccount {
                name = "salted-storage"
                password = oldInput
            } as AccountInventory
            String stored = dbFindByUuid(salted.uuid, AccountVO.class).password
            assert AccountPassword.isEncoded(stored)
            assert AccountPassword.matches(oldInput, stored)

            session = logInByAccount {
                accountName = salted.name
                password = oldInput
            } as SessionInventory
            assert session.accountUuid == salted.uuid
            expectApiFailure({
                logInByAccount {
                    accountName = salted.name
                    password = stored
                }
            }) { assert code != null }
            expectApiFailure({
                updateAccount {
                    uuid = salted.uuid
                    oldPassword = "wrong"
                    password = newInput
                    sessionId = admin.uuid
                }
            }) { assert code != null }
            updateAccount {
                uuid = salted.uuid
                oldPassword = oldInput
                password = newInput
                sessionId = admin.uuid
            }
            assert AccountPassword.matches(newInput, dbFindByUuid(salted.uuid, AccountVO.class).password)
            expectApiFailure({
                logInByAccount {
                    accountName = salted.name
                    password = oldInput
                }
            }) { assert code != null }

            def factory = bean(TwoFactorAuthenticationAccountFactory.class)
            assert factory.createAuthentication(new TwoFactorAuthenticationParamStruct(
                    name: salted.name, password: newInput)).accountUuid == salted.uuid
            assert factory.createAuthentication(new TwoFactorAuthenticationParamStruct(
                    name: salted.name, password: "wrong")) == null

            retryInSecs {
                List<String> history = Q.New(HistoricalPasswordVO.class)
                        .eq(HistoricalPasswordVO_.uuid, salted.uuid)
                        .select(HistoricalPasswordVO_.password).listValues()
                assert history.any { AccountPassword.isEncoded(it) && AccountPassword.matches(oldInput, it) }
            }
            String oldHistory = DigestUtils.sha512Hex("legacy-history")
            bean(DatabaseFacade.class).persist(new HistoricalPasswordVO(uuid: salted.uuid, password: oldHistory))
            updateGlobalConfig {
                category = PasswordStrategyGlobalConfig.CATEGORY
                name = PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.name
                value = "true"
            }
            [oldInput, oldHistory, newInput].each { repeated ->
                expectApiFailure({
                    updateAccount {
                        uuid = salted.uuid
                        oldPassword = newInput
                        password = repeated
                        sessionId = admin.uuid
                    }
                }) { assert code != null }
            }

            setStorage(false)
            session = logInByAccount {
                accountName = salted.name
                password = newInput
            } as SessionInventory
            assert session.accountUuid == salted.uuid
        }
    }

    @Override
    void clean() {
        try {
            if (originalConfig != null) {
                updateGlobalConfig {
                    category = IdentityGlobalConfig.CATEGORY
                    name = IdentityGlobalConfig.ENABLE_SALTED_PASSWORD.name
                    value = originalConfig
                }
            }
            if (originalHistoryConfig != null) {
                updateGlobalConfig {
                    category = PasswordStrategyGlobalConfig.CATEGORY
                    name = PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.name
                    value = originalHistoryConfig
                }
            }
        } finally {
            env.delete()
        }
    }
}
