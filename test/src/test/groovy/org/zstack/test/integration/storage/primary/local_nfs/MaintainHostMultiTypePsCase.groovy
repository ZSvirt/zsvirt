package org.zstack.test.integration.storage.primary.local_nfs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.HostStateEvent
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by MaJin on 2018/1/24.
 */
class MaintainHostMultiTypePsCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm1, vm1OnNfs, vm2OnNfs, vmOnLocal
    HostInventory host1, host2
    PrimaryStorageInventory local, nfs

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageNfsOneVmEnv()
    }

    @Override
    void test() {
       env.create {
           vm1 = env.inventoryByName("vm") as VmInstanceInventory
           host1 = env.inventoryByName("kvm") as HostInventory
           nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
           local = env.inventoryByName("local") as PrimaryStorageInventory
           prepareEnv()
           testMaintainHost()
       }
    }

    @Override
    void clean() {
        env.delete()
    }


    void prepareEnv(){
        def instanceOffering = createInstanceOffering {
            name = "1C1G"
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
        } as InstanceOfferingInventory

        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        host2 = addKVMHost {
            name = "kvm2"
            managementIp = "127.0.0.2"
            username = "root"
            password = "password"
            clusterUuid = host1.clusterUuid
        } as HostInventory

        vm1OnNfs = createVmInstance {
            name = "test2"
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        vm2OnNfs = createVmInstance {
            name = "test2"
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        vmOnLocal = createVmInstance {
            name = "test3"
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = local.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory
    }

    void testMaintainHost(){
        env.simulator(KVMConstant.KVM_VM_SYNC_PATH){
            def rsp = new KVMAgentCommands.VmSyncResponse()
            rsp.setError("on purpose")
            return rsp
        }

        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH){ HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.MigrateVmCmd)
            def rsp = new KVMAgentCommands.MigrateVmResponse()
            if (cmd.vmUuid == vm2OnNfs.uuid){
                rsp.setError("on purpose")
            }
            return rsp
        }

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm1.uuid).set(VmInstanceVO_.state, VmInstanceState.Unknown).update()

        changeHostState {
            uuid = host1.uuid
            stateEvent = HostStateEvent.maintain
        }

        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm1.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Unknown
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm1OnNfs.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm2OnNfs.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Stopped
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmOnLocal.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Stopped
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.hostUuid, host1.uuid).count() == 1
    }
}
