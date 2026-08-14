package org.zstack.test.integration.storage.primary.local

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.kvm.KvmVmSyncPingTask
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetVmCapabilitiesResult
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*

/**
 * Created by zouye on 2017/2/28.
 */
class LocalStorageMigrateVolumeCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageMigrateVolumeCapabilities()
            testLocalStorageMigrateVolumeWhenDisable()
            testLocalStorageMigrateVolume()
        }

    }

    void testLocalStorageMigrateVolumeCapabilities() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def volume = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = volume.uuid
        }

        // with data volumes can not live migrate and volume migrate
        GetVmCapabilitiesResult capRes = getVmCapabilities {
            uuid = vm.uuid
        }

        assert !capRes.capabilities.get(VmInstanceConstant.Capability.LiveMigration.toString()) as Boolean
        assert !capRes.capabilities.get(VmInstanceConstant.Capability.VolumeMigration.toString()) as Boolean

        detachDataVolumeFromVm {
            uuid = volume.uuid
            vmUuid = vm.uuid
        }
    }

    void testLocalStorageMigrateVolumeWhenDisable() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("vm")
        HostSpec hostSpec = env.specByName("kvm")
        HostSpec hostSpec1 = env.specByName("kvm1")
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        assert vmSpec.inventory.rootVolumeUuid
        assert hostSpec.inventory.uuid

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled

        stopVmInstance {
            uuid = vmSpec.inventory.uuid
        }

        LocalStorageMigrateVolumeAction action = new LocalStorageMigrateVolumeAction()
        action.volumeUuid = vmSpec.inventory.rootVolumeUuid
        action.destHostUuid = hostSpec1.inventory.uuid
        action.sessionId = Test.currentEnvSpec.session.uuid

        LocalStorageMigrateVolumeAction.Result res = action.call()
        assert res.error != null
    }

    void testLocalStorageMigrateVolume() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("vm")
        HostSpec hostSpec1 = env.specByName("kvm1")
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        KvmVmSyncPingTask pingTask = bean(KvmVmSyncPingTask.class)

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled

        boolean calledCheckMD5 = false
        env.afterSimulator(LocalStorageKvmBackend.CHECK_MD5_PATH) { rsp ->
            calledCheckMD5 = true
            return rsp
        }

        boolean calledCopyToRemote = false
        env.afterSimulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { rsp ->
            assert pingTask.vmsToSkip[Platform.getManagementServerId()].contains(vmSpec.inventory.uuid)

            calledCopyToRemote = true
            return rsp
        }

        assert !pingTask.vmsToSkip[Platform.getManagementServerId()].contains(vmSpec.inventory.uuid)

        localStorageMigrateVolume {
            volumeUuid = vmSpec.inventory.rootVolumeUuid
            destHostUuid = hostSpec1.inventory.uuid
        }

        assert !pingTask.vmsToSkip[Platform.getManagementServerId()].contains(vmSpec.inventory.uuid)
        assert calledCheckMD5 && calledCopyToRemote
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
