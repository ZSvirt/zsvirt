package org.zstack.test.integration.storage.primary.nfs.capacity

import org.springframework.http.HttpEntity
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.storage.primary.nfs.NfsPrimaryToSftpBackupKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.testlib.VmSpec
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.core.db.DatabaseFacade
import org.zstack.kvm.KVMAgentCommands
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend

/**
 * Created by SyZhao on 2017/4/21.
 */
class NfsExpungeVmByImageReconnectCapacityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testExpungeVmByImageReconnectCheckCapacity()
        }
    }

    void testExpungeVmByImageReconnectCheckCapacity() {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString())
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmSpec vmSpec = env.specByName("vm")
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        assert vmSpec.inventory.rootVolumeUuid

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        int spend = 1000000000
        boolean checked = false
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity - spend 
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availablePhysicalCapacity ==  afterCapacityResult.availablePhysicalCapacity + spend

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        destroyVmInstance {
            uuid = vmSpec.inventory.uuid
        }
        assert destroyCmd != null
        assert destroyCmd.uuid == vmSpec.inventory.uuid
        VmInstanceVO vmvo = dbFindByUuid(destroyCmd.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Destroyed


        def delete_path_invoked = false
        env.simulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            delete_path_invoked = true
            NfsPrimaryStorageSpec nspec = spec.specByUuid(ps.uuid)

            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.totalCapacity = nspec.totalCapacity
            rsp.availableCapacity = nspec.availableCapacity + spend
            delete_bits_path_is_invoked = true
            return rsp
        }
        expungeVmInstance {
            uuid = vmSpec.inventory.uuid
        }
        assert delete_path_invoked

        ////GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
        ////    primaryStorageUuids = [ps.uuid]
        ////}
        ////assert beforeCapacityResult.availableCapacity < capacityResult.availableCapacity
        ////assert beforeCapacityResult.availablePhysicalCapacity < capacityResult.availablePhysicalCapacity

        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity 
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity
        //assert beforeCapacityResult.availableCapacity == afterCapacityResult.availableCapacity

    }
}
