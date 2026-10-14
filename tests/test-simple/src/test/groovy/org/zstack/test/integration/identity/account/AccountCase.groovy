package org.zstack.test.integration.identity.account

import org.zstack.core.db.SQL
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountVO_
import org.zstack.identity.AccountLoginBackend
import org.zstack.identity.AccountManagerImpl
import org.zstack.loginControl.PasswordStrategyGlobalConfig
import org.zstack.loginControl.entity.HistoricalPasswordVO
import org.zstack.loginControl.entity.HistoricalPasswordVO_
import org.zstack.twoFactorAuthentication.TwoFactorAuthenticationAccountFactory
import org.zstack.twoFactorAuthentication.TwoFactorAuthenticationParamStruct
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmQuotaGlobalConfig
import org.zstack.core.config.GlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountType
import org.zstack.header.identity.AccountVO
import org.zstack.header.identity.QuotaVO
import org.zstack.header.identity.QuotaVO_
import org.zstack.header.identity.SessionVO
import org.zstack.header.identity.SessionVO_
import org.zstack.identity.AccountPassword
import org.zstack.identity.QuotaGlobalConfig
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateAccountAction
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.sql.Timestamp

/**
 * Created by AlanJager on 2017/3/23.
 */
class AccountCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory
    AccountInventory accountInventory1
    String originalHistoryConfig

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

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        env = IdentityTest.oneVmBasicEnv()
    }

    @Override
    void test() {
        originalHistoryConfig = PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.value()
        env.create {
            testInitialAdminPasswordStorage()
            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory

            accountInventory1 = createAccount {
                name = "test1"
                password = "password"
            } as AccountInventory

            testNormalAccountQueryGlobalConfig()
            testLoginAsAdminAccountAndChangeSelfPassword()
            testLoginAsNormalAccountAndChangeSelfPassword()
            testNormalAccountCannotDeleteAnyAccount()
            testAdminAccountDeleteSystemAdmin()
            testCreateAccount()
            testQuotaConfig()
            testAPIOperationRenewSession()
            testLegacyPasswordCompatibility()
            testSaltedPasswordStorage()
        }
    }

    void testInitialAdminPasswordStorage() {
        // Verify the admin initialized by the existing identity test suite.
        def admin = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        String stored = admin.password
        def lastOpDate = admin.lastOpDate
        assert AccountPassword.isEncoded(stored)
        assert AccountPassword.matches(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD, stored)
        assert loginAsAdmin().accountUuid == admin.uuid

        2.times {
            bean(AccountManagerImpl.class).prepareDbInitialValue()
            def unchanged = dbFindByUuid(admin.uuid, AccountVO.class)
            assert unchanged.password == stored
            assert unchanged.lastOpDate == lastOpDate
            assert loginAsAdmin().accountUuid == admin.uuid
        }
    }

    void testLegacyPasswordCompatibility() {
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
    }

    void testSaltedPasswordStorage() {
        SessionInventory admin = loginAsAdmin()
        String oldInput = DigestUtils.sha512Hex("legacy-password")
        String newInput = DigestUtils.sha512Hex("new-password")
        AccountInventory salted = createAccount {
            name = "salted-storage"
            password = oldInput
        } as AccountInventory
        String stored = dbFindByUuid(salted.uuid, AccountVO.class).password
        assert AccountPassword.isEncoded(stored)
        assert AccountPassword.matches(oldInput, stored)

        SessionInventory session = logInByAccount {
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

    void testAPIOperationRenewSession() {
        updateGlobalConfig {
            category = QuotaGlobalConfig.CATEGORY
            name = VmQuotaGlobalConfig.VM_TOTAL_NUM.name
            value = 3
        }

        def image = env.inventoryByName("image1" ) as ImageInventory
        def l3 = env.inventoryByName("pubL3") as L3NetworkInventory

        SessionInventory s2 = loginAsAdmin()

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            sleep(2000)
            return rsp
        }

        createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            cpuNum = 4
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            l3NetworkUuids = [l3.uuid]
            sessionId = s2.uuid
        }

        createVmInstance {
            name = "vm-2"
            imageUuid = image.uuid
            cpuNum = 4
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            l3NetworkUuids = [l3.uuid]
            sessionId = s2.uuid
        }

        def time1 = s2.expiredDate

        def time2 = Q.New(SessionVO.class)
                .eq(SessionVO_.uuid, s2.uuid)
                .select(SessionVO_.expiredDate)
                .findValue() as Timestamp

        assert time1.before(time2)
    }

    void testNormalAccountQueryGlobalConfig() {
        createAccount {
            name = "accountQueryGlobalConfig"
            password = "password"
        }

        SessionInventory s = logInByAccount {
            accountName = "accountQueryGlobalConfig"
            password = "password"
        }

        queryGlobalConfig {
            conditions = []
            sessionId = s.uuid
        }
    }

    void testLoginAsAdminAccountAndChangeSelfPassword() {
        SessionInventory sessionInventory = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory

        // change
        updateAccount {
            uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
            password = "new"
            sessionId = sessionInventory.uuid
        }

        // update password will force logout
        sessionInventory = logInByAccount {
            delegate.accountName = "admin"
            delegate.password = "new"
        } as SessionInventory

        AccountVO accountVO = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        assert AccountPassword.isEncoded(accountVO.password)
        assert AccountPassword.matches("new", accountVO.password)

        //changeWithRightOldPassword
        updateAccount {
            uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
            password = "new2"
            sessionId = sessionInventory.uuid
            oldPassword = "new"
        }

        // update password will force logout
        sessionInventory = logInByAccount {
            delegate.accountName = "admin"
            delegate.password = "new2"
        } as SessionInventory

        accountVO = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        assert AccountPassword.isEncoded(accountVO.password)
        assert AccountPassword.matches("new2", accountVO.password)

        //changeWithWrongOldPassword
        UpdateAccountAction updateAccountAction = new UpdateAccountAction()
        updateAccountAction.uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        updateAccountAction.password = "new3"
        updateAccountAction.sessionId = sessionInventory.uuid
        updateAccountAction.oldPassword = "wrongPassword"
        UpdateAccountAction.Result result = updateAccountAction.call()
        assert result.error != null

        // restore
        updateAccount {
            uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
            sessionId = sessionInventory.uuid
        }

        AccountVO accountVO1 = dbFindByUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, AccountVO.class)
        assert AccountPassword.isEncoded(accountVO1.password)
        assert AccountPassword.matches(AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD, accountVO1.password)
    }

    void testLoginAsNormalAccountAndChangeSelfPassword() {
        SessionInventory sessionInventory = logInByAccount {
            accountName = accountInventory.name
            password = "password"
        } as SessionInventory

        updateAccount {
            uuid = accountInventory.uuid
            password = "new"
            sessionId = sessionInventory.uuid
        }

        AccountVO accountVO = dbFindByUuid(accountInventory.uuid, AccountVO.class)
        assert AccountPassword.isEncoded(accountVO.password)
        assert AccountPassword.matches("new", accountVO.password)

        logOut {
            sessionUuid = sessionInventory.uuid
        }
    }

    void testNormalAccountCannotDeleteAnyAccount() {
        // login as normal account
        SessionInventory sessionInventory = logInByAccount {
            accountName = accountInventory.name
            password = "new"
        } as SessionInventory

        // delete self
        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            deleteAccount {
                uuid = accountInventory.uuid
                sessionId = sessionInventory.uuid
            }
        }

        // delete another normal account
        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            deleteAccount {
                uuid = accountInventory1.uuid
                sessionId = sessionInventory.uuid
            }
        }

        // delete admin account
        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            deleteAccount {
                uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
                sessionId = sessionInventory.uuid
            }
        }

        // login as admin and delete normal account
        SessionInventory adminSessionInv = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory

        deleteAccount {
            uuid = accountInventory.uuid
            sessionId = adminSessionInv.uuid
        }
        deleteAccount {
            uuid = accountInventory1.uuid
            sessionId = adminSessionInv.uuid
        }
        assert null == dbFindByUuid(accountInventory.uuid, AccountVO.class)
        assert null == dbFindByUuid(accountInventory1.uuid, AccountVO.class)

        // delete admin account
        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            deleteAccount {
                uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
                sessionId = adminSessionInv.uuid
            }
        }
    }

    void testAdminAccountDeleteSystemAdmin() {
        env.session = logInByAccount {
            delegate.accountName = "admin"
            delegate.password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory

        def userpass = "password"
        def newAdmin = createAccount {
            name = "testAdmAccount"
            password = userpass
            type = AccountType.SystemAdmin.toString()
        } as AccountInventory

        SessionInventory adminSessionInv = logInByAccount {
            accountName = newAdmin.name
            password = userpass
        } as SessionInventory

        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            deleteAccount {
                sessionId = adminSessionInv.uuid
                uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
            }
        }
    }

    void testCreateAccount(){
        def account1 = createAccount {
            name = "testAccount1"
            password = "password"
        } as AccountInventory

        testUpdateQuotaGlobalConfig(VmQuotaGlobalConfig.VM_TOTAL_NUM.getName())

        def account2 = createAccount {
            name = "testAccount2"
            password = "password"
        } as AccountInventory

        assert Q.New(QuotaVO.class).select(QuotaVO_.value)
                .eq(QuotaVO_.identityUuid, account1.uuid).eq(QuotaVO_.@name, VmQuotaGlobalConfig.VM_TOTAL_NUM.name)
                .findValue() == VmQuotaGlobalConfig.VM_TOTAL_NUM.defaultValue(Long.class)
        assert Q.New(QuotaVO.class).select(QuotaVO_.value)
                .eq(QuotaVO_.identityUuid, account2.uuid).eq(QuotaVO_.@name, VmQuotaGlobalConfig.VM_TOTAL_NUM.name)
                .findValue() == 1
    }

    void testQuotaConfig(){
        Field[] fields = QuotaGlobalConfig.class.fields
        for (Field f : fields){
            if (!GlobalConfig.class.isAssignableFrom(f.getType()) || !Modifier.isStatic(f.getModifiers())){
                continue
            }
            testUpdateQuotaGlobalConfig(((GlobalConfig)f.get(null)).name)
        }
    }

    private testUpdateQuotaGlobalConfig(String configName){
        def a = new UpdateGlobalConfigAction()
        a.category = QuotaGlobalConfig.CATEGORY
        a.name = configName
        a.value = "1.0"
        a.sessionId = adminSession()
        assert a.call().error != null

        a = new UpdateGlobalConfigAction()
        a.category = QuotaGlobalConfig.CATEGORY
        a.name = configName
        a.value = "-1"
        a.sessionId = adminSession()
        assert a.call().error != null

        updateGlobalConfig {
            category = QuotaGlobalConfig.CATEGORY
            name = configName
            value = "0"
        }

        updateGlobalConfig {
            category = QuotaGlobalConfig.CATEGORY
            name = configName
            value = "1"
        }
    }
}
