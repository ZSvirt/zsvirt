package org.zstack.test.integration.identity.resource

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountConstant
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.identity.role.RoleInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.header.errorcode.SysErrors.RESOURCE_NOT_ACCESSIBLE

/**
 * Created by camile on 2017/6/11.
 */
class OperationResourceCase extends SubCase {
    EnvSpec envSpec
    AccountInventory accountInventory

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        envSpec = Env.oneVmBasicEnv()
    }

    void testChangeOwnerWhenVMStarting() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm = envSpec.inventoryByName("vm")
        accountInventory = createAccount {
            name = "test"
            password = "password"
        }
        VmInstanceVO vmVo = dbf.findByUuid(vm.uuid, VmInstanceVO.class)
        vmVo.setState(VmInstanceState.Starting)
        vmVo = dbf.updateAndRefresh(vmVo)

        changeResourceOwner{
            accountUuid = accountInventory.uuid
            resourceUuid = vmVo.uuid
        }

        accountInventory = createAccount {
            name = "test2"
            password = "password"
        }

        vmVo.setState(VmInstanceState.Paused)
        vmVo = dbf.updateAndRefresh(vmVo)
        changeResourceOwner {
            accountUuid = accountInventory.uuid
            resourceUuid = vmVo.uuid
        }
    }

    void testShareResourceAgain(){
        logger.info("Test 011: share resource admin will not raise error")
        logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        }

        def bs = envSpec.inventoryByName("sftp") as BackupStorageInventory
        def image11 = addImage {
            name = "image11"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        } as ImageInventory

        shareResource {
            resourceUuids = [image11.uuid]
            accountUuids = [accountInventory.uuid]
        }

        // again
        shareResource {
            resourceUuids = [image11.uuid]
            accountUuids = [accountInventory.uuid]
        }

        // to public first, then share to account
        shareResource {
            resourceUuids = [image11.uuid]
            toPublic = true
        }

        shareResource {
            resourceUuids = [image11.uuid]
            accountUuids = [accountInventory.uuid]
        }

        revokeResourceSharing{
            resourceUuids = [image11.uuid]
            accountUuids = [accountInventory.uuid]
        }

        // revoke again
        revokeResourceSharing{
            resourceUuids = [image11.uuid]
            accountUuids = [accountInventory.uuid]
        }
    }

    void testSharedResourceOperator() {
        def accountTestUuid = (queryAccount {
            delegate.conditions = [
                "name=test"
            ]
        } as List<AccountInventory>)[0]
        assert accountTestUuid != null

        def accountTest2Uuid = (queryAccount {
            delegate.conditions = [
                "name=test2"
            ]
        } as List<AccountInventory>)[0]
        assert accountTest2Uuid != null

        def role1 = createRole {
            delegate.name = "role1"
            delegate.policies = [
                ".header.image.APIUpdateImageMsg",
            ]
        } as RoleInventory

        attachRoleToAccount {
            delegate.roleUuid = role1.uuid
            delegate.accountUuid = accountTestUuid.uuid
        }

        attachRoleToAccount {
            delegate.roleUuid = role1.uuid
            delegate.accountUuid = accountTest2Uuid.uuid
        }

        logger.info("Test 021: share resource to account: test2, not account: test")

        def image = envSpec.inventoryByName("image1") as ImageInventory
        shareResource {
            resourceUuids = [image.uuid]
            accountUuids = [accountTest2Uuid.uuid]
        }

        withAccountSession("test", "password") {
            expectApiFailure({
                updateImage {
                    delegate.uuid = image.uuid
                    delegate.name = "updated"
                }
            }) {
                assert delegate.code == "SYS.1018"
                assert RESOURCE_NOT_ACCESSIBLE.toString() == "SYS.1018"
            }
        }

        withAccountSession("test2", "password") {
            updateImage {
                delegate.uuid = image.uuid
                delegate.name = "updated2"
            }
        }
    }

    @Override
    void test() {
        envSpec.create {
            testChangeOwnerWhenVMStarting()
            testShareResourceAgain()
            testSharedResourceOperator()
        }
    }
}
