package org.zstack.test.integration.storage.primary.local.psmaintenance

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeStatus
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageMaintenancePrimaryStorageDeleteVmCase extends SubCase{
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
            testLocalStorageDeleteVmWhenMaintained()
        }
    }


    void testLocalStorageDeleteVmWhenMaintained() {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString())

        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("vm")
        HostSpec hostSpec = env.specByName("kvm")
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        assert vmSpec.inventory.rootVolumeUuid
        assert hostSpec.inventory.uuid

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Maintenance


        retryInSecs {
            assert dbFindByUuid(vmSpec.inventory.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        }
        List<String> destroyRequests = []
        List<String> physicalDeletes = []
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            destroyRequests.add(cmd.uuid)
            return rsp
        }
        [LocalStorageKvmBackend.DELETE_BITS_PATH, LocalStorageKvmBackend.DELETE_DIR_PATH].each { path ->
            env.afterSimulator(path) { rsp, HttpEntity<String> e ->
                if (e.body.contains(vmSpec.inventory.rootVolumeUuid)) {
                    physicalDeletes.add(e.body)
                }
                return rsp
            }
        }
        destroyVmInstance { uuid = vmSpec.inventory.uuid }

        assert destroyRequests == [vmSpec.inventory.uuid]
        assert dbFindByUuid(vmSpec.inventory.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert dbFindByUuid(vmSpec.inventory.rootVolumeUuid, VolumeVO.class).status == VolumeStatus.Deleted
        assert physicalDeletes.isEmpty()
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Maintenance

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }


        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
