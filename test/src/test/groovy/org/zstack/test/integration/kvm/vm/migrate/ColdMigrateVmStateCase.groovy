package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-06-21.
 */
class ColdMigrateVmStateCase extends SubCase{
    EnvSpec env
    HostInventory host1, host2
    VmInstanceInventory vm
    PrimaryStorageInventory ps

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoHostLocalEnv()
    }

    @Override
    void test() {
        env.create {
            host1 = env.inventoryByName("host1") as HostInventory
            host2 = env.inventoryByName("host2") as HostInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            ps = env.inventoryByName("local") as PrimaryStorageInventory
            testVolumeMigratingStateWork()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVolumeMigratingStateWork(){
        String originHostUuid = vm.hostUuid
        String dstHostUuid = host1.uuid == originHostUuid ? host2.uuid : host1.uuid

        stopVmInstance {
            uuid = vm.uuid
        }

        boolean call = false
        env.simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {
            call = true
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .select(VmInstanceVO_.state).findValue() == VmInstanceState.VolumeMigrating
            // now vm is in SyncTaskChain

            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
        }

        localStorageMigrateVolume {
            volumeUuid = vm.rootVolumeUuid
            destHostUuid = dstHostUuid
        }
        assert call

        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .select(VmInstanceVO_.state).findValue() == VmInstanceState.Stopped
    }
}
