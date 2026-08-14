package org.zstack.test.integration.identity.account

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
import org.zstack.identity.QuotaGlobalConfig
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateAccountAction
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
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

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
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
        }
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
        assert accountVO.password  == "new"

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
        assert accountVO.password  == "new2"

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
        assert accountVO1.password == AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
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
        assert accountVO.password == "new"

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
