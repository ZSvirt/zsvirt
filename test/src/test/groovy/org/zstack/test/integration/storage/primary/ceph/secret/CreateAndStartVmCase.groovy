package org.zstack.test.integration.storage.primary.ceph.secret

import org.springframework.http.HttpEntity
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.tag.SystemTagInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/05/09.
 */
class CreateAndStartVmCase extends SubCase{
    EnvSpec env
    PrimaryStorageInventory ps

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
            testStartVmAuthWhenNocephxSetted(false)
            testCreateVmAuthWhenNocephxSetted(false)
            testStartVmAuthWhenNocephxSetted(true)
            testCreateVmAuthWhenNocephxSetted(true)
        }
    }

    void testStartVmAuthWhenNocephxSetted(boolean nocephx){
        setNocephx(nocephx)

        VmInstanceInventory vm = env.inventoryByName("test-vm")
        String psuuid = vm.allVolumes[0].primaryStorageUuid

        stopVmInstance {
            uuid = vm.uuid
        }

        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }
        assert null != cmd
        if (nocephx){
            assert cmd.addons.get(CephConstants.CEPH_SCECRET_KEY) == null
            assert cmd.addons.get(CephConstants.CEPH_SECRECT_UUID) == null
        } else {
            assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.addons.get(CephConstants.CEPH_SCECRET_KEY)
            assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.addons.get(CephConstants.CEPH_SECRECT_UUID)
        }
    }

    void testCreateVmAuthWhenNocephxSetted(boolean nocephx){
        setNocephx(nocephx)

        VmInstanceInventory vm = env.inventoryByName("test-vm")
        String psuuid = vm.allVolumes[0].primaryStorageUuid

        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        createVmInstance {
            name = "newVm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
        }

        assert null != cmd
        if (nocephx){
            assert cmd.addons.get(CephConstants.CEPH_SCECRET_KEY) == null
            assert cmd.addons.get(CephConstants.CEPH_SECRECT_UUID) == null
        } else {
            assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.addons.get(CephConstants.CEPH_SCECRET_KEY)
            assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.addons.get(CephConstants.CEPH_SECRECT_UUID)
        }
    }

    private setNocephx(boolean nocephx){
        if (nocephx){
            createSystemTag {
                resourceType = PrimaryStorageVO.class.simpleName
                resourceUuid = ps.uuid
                tag = CephSystemTags.NO_CEPHX_TOKEN
            }
        } else {
            def tags = (querySystemTag {
                delegate.conditions = ["tag=$CephSystemTags.NO_CEPHX_TOKEN}".toString()]
            } as List<SystemTagInventory>)

            if (!tags.isEmpty()){
                deleteTag {
                    uuid = tags.get(0).uuid
                }
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}