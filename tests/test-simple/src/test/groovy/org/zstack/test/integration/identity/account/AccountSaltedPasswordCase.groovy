package org.zstack.test.integration.identity.account

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountVO
import org.zstack.header.identity.AccountVO_
import org.zstack.identity.AccountLoginBackend
import org.zstack.identity.AccountManagerImpl
import org.zstack.identity.AccountPassword
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

    @Override
    void test() {
        originalHistoryConfig = PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.value()
        env.create {
            SessionInventory admin = loginAsAdmin()
            def existingAdmin = dbFindByUuid(admin.accountUuid, AccountVO.class)
            String existingAdminPassword = existingAdmin.password
            def existingAdminLastOpDate = existingAdmin.lastOpDate
            bean(AccountManagerImpl.class).prepareDbInitialValue()
            def unchangedAdmin = dbFindByUuid(admin.accountUuid, AccountVO.class)
            assert unchangedAdmin.password == existingAdminPassword
            assert unchangedAdmin.lastOpDate == existingAdminLastOpDate
            String oldInput = DigestUtils.sha512Hex("legacy-password")
            String newInput = DigestUtils.sha512Hex("new-password")
            AccountInventory legacy = createAccount {
                name = "legacy-storage"
                password = oldInput
            } as AccountInventory
            // Model an account stored by the previous version; new API writes are always salted.
            SQL.New(AccountVO.class).eq(AccountVO_.uuid, legacy.uuid)
                    .set(AccountVO_.password, oldInput).update()
            def legacyBeforeLogin = dbFindByUuid(legacy.uuid, AccountVO.class)
            assert legacyBeforeLogin.password == oldInput
            def legacyLastOpDate = legacyBeforeLogin.lastOpDate
            String historicalInput = DigestUtils.sha512Hex("pre-upgrade-history")
            HistoricalPasswordVO legacyHistory = bean(DatabaseFacade.class).persist(
                    new HistoricalPasswordVO(uuid: legacy.uuid, password: historicalInput,
                            createDate: new java.sql.Timestamp(946684800000L)))
            def historyDate = dbFindById(legacyHistory.id, HistoricalPasswordVO.class).createDate

            // Reinitialization and login must leave pre-upgrade credentials untouched.
            bean(AccountManagerImpl.class).prepareDbInitialValue()
            assert dbFindByUuid(legacy.uuid, AccountVO.class).password == oldInput
            assert dbFindByUuid(legacy.uuid, AccountVO.class).lastOpDate == legacyLastOpDate

            // Legacy login keeps the database's existing comparison semantics.
            boolean oldDatabaseMatch = Q.New(AccountVO.class).eq(AccountVO_.uuid, legacy.uuid)
                    .eq(AccountVO_.password, oldInput.toUpperCase()).isExists()
            assert bean(AccountLoginBackend.class).authenticate(legacy.name, oldInput.toUpperCase()) == oldDatabaseMatch

            SessionInventory session = logInByAccount {
                accountName = legacy.name
                password = oldInput
            } as SessionInventory
            assert session.accountUuid == legacy.uuid
            assert dbFindByUuid(legacy.uuid, AccountVO.class).password == oldInput
            assert dbFindByUuid(legacy.uuid, AccountVO.class).lastOpDate == legacyLastOpDate
            assert dbFindById(legacyHistory.id, HistoricalPasswordVO.class).password == historicalInput
            assert dbFindById(legacyHistory.id, HistoricalPasswordVO.class).createDate == historyDate

            updateAccount {
                uuid = legacy.uuid
                oldPassword = oldInput
                password = newInput
                sessionId = session.uuid
            }
            String changedLegacy = dbFindByUuid(legacy.uuid, AccountVO.class).password
            assert AccountPassword.isEncoded(changedLegacy)
            assert AccountPassword.matches(newInput, changedLegacy)
            retryInSecs {
                List<String> history = Q.New(HistoricalPasswordVO.class)
                        .eq(HistoricalPasswordVO_.uuid, legacy.uuid)
                        .select(HistoricalPasswordVO_.password).listValues()
                assert history.any { AccountPassword.isEncoded(it) && AccountPassword.matches(oldInput, it) }
            }
            assert dbFindById(legacyHistory.id, HistoricalPasswordVO.class).password == historicalInput
            assert dbFindById(legacyHistory.id, HistoricalPasswordVO.class).createDate == historyDate
            assert (logInByAccount {
                accountName = legacy.name
                password = newInput
            } as SessionInventory).accountUuid == legacy.uuid
            expectApiFailure({
                logInByAccount {
                    accountName = legacy.name
                    password = oldInput
                }
            }) { assert code != null }

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
