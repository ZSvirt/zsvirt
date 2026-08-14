package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.SQL
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/26.
 */
class RootVolumeDeletedFromDbCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    void testQueryVmWithRootVolumeDeleted() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // hard delete the root volume to make a
        // not integrity VM in DB
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).delete()

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        }[0]

        // no exception happen
    }

    @Override
    void test() {
        env.create {
            testQueryVmWithRootVolumeDeleted()
        }
    }
}
