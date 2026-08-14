package org.zstack.test.integration.kvm.nic

import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkStateEvent
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by lining on 2018/5/19.
 */
class VmNicBasicCase extends SubCase {
    EnvSpec env

    VmNicInventory nic
    String usedIpUuid

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
            testCreateVmNic()
            testAttachVmNicToVm()
            testDetachVmNic()
            testDeleteVmNic()
        }
    }

    void testCreateVmNic() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        // an invalid static ip
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
                ip = "0.0.0.1"
            }
        }

        changeL3NetworkState {
            uuid = pubL3.uuid
            stateEvent = L3NetworkStateEvent.disable
        }

        // attach a disable network to vm nic
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
            }
        }

        changeL3NetworkState {
            uuid = pubL3.uuid
            stateEvent = L3NetworkStateEvent.enable
        }

        nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        assert nic.uuid != null
        assert nic.l3NetworkUuid == pubL3.uuid
        assert nic.deviceId == 1
        assert nic.vmInstanceUuid == null
        assert nic.ip != null
        assert nic.mac != null
        assert nic.usedIps.size() != 0

        IpRangeInventory ipRangeInventory = pubL3.ipRanges.get(0)
        assert nic.gateway == ipRangeInventory.gateway
        assert nic.netmask == ipRangeInventory.netmask

        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.vmNicUuid, nic.uuid).isExists()

        // an used static ip
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
                ip = nic.ip
            }
        }
    }

    void testAttachVmNicToVm () {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        changeL3NetworkState {
            uuid = nic.l3NetworkUuid
            stateEvent = L3NetworkStateEvent.disable
        }

        // attach a disable network to vm
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

        changeL3NetworkState {
            uuid = nic.l3NetworkUuid
            stateEvent = L3NetworkStateEvent.enable
        }

        pauseVmInstance {
            uuid = vm.uuid
        }

        // attach the nic to a vm state paused
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

        resumeVmInstance {
            uuid = vm.uuid
        }

        vm.defaultL3NetworkUuid = l3.uuid
        vm = attachVmNicToVm {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nic.uuid
        }
        vm.defaultL3NetworkUuid = l3.uuid

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)

        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO.deviceId == 1
        assert vmNicVO.internalName == VmNicVO.generateNicInternalName(vmInstanceVO.getInternalId(), 1)
        assert vmNicVO.vmInstanceUuid == vm.getUuid()

        usedIpUuid = vmNicVO.usedIpUuid

        // re-attach the same nic
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

    }

    void testDetachVmNic() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        detachL3NetworkFromVm {
            vmNicUuid = nic.uuid
            systemTags = [VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC.instantiateTag([(VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC_TOKEN): false])]
        }

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
        vmInstanceVO.defaultL3NetworkUuid = l3.uuid

        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO.deviceId == 1
        assert vmNicVO.internalName == null
        assert vmNicVO.vmInstanceUuid == null
    }

    void testDeleteVmNic() {
        deleteVmNic {
            uuid = nic.uuid
        }
        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO == null

        UsedIpVO usedIpVO = dbFindByUuid(usedIpUuid, UsedIpVO.class)
        assert usedIpVO == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
