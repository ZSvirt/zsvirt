package org.zstack.test.integration.identity.resource

import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/5/24.
 */
class GetResourceAccountCase extends SubCase {
    EnvSpec envSpec

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

    void testGetResourceAccount() {
        VmInstanceInventory vm = envSpec.inventoryByName("vm")

        Map accountInventories = getResourceAccount {
            resourceUuids = [vm.uuid]
        }
        assert 1 == accountInventories.size()
        assert Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, vm.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .select(AccountResourceRefVO_.accountUuid)
                .findValue() == accountInventories.get(vm.uuid).uuid
    }

    @Override
    void test() {
        envSpec.create {
            testGetResourceAccount()
        }
    }
}
