package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/15.
 */
class ExpungeVmWhenRootVolumeNotExistCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory

            testRootVolumeRecordDeletedExpungeVmStillSuccess()
        }
    }

    void testRootVolumeRecordDeletedExpungeVmStillSuccess() {
        destroyVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Destroyed

        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vo.getRootVolumeUuid()).delete()
        assert !Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vo.getRootVolumeUuid()).exists

        expungeVmInstance {
            uuid = vm.uuid
        }
    }
}
