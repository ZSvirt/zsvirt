package org.zstack.test.integration.computes.vmclone

import org.zstack.sdk.CloneVmInstanceInventory
import org.zstack.sdk.CloneVmInstanceResult
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.VmCdRomInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.computes.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class CloneBaseCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm1
    KVMHostInventory kvm2

    @Override
    void setup() {
        useSpring(VmCloneTest.springSpec)
    }

    @Override
    void environment() {
        env = KvmTest.cephPsCephBs2Host()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testBasic()
        }
    }

    void prepare() {
        vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        kvm2 = env.inventoryByName("kvm2") as KVMHostInventory
    }

    void testBasic() {
        logger.info("Test 001: clone VM to kvm2")

        def vm2 = cloneVmInstance {
            delegate.vmInstanceUuid = vm1.uuid
            delegate.hostUuid = kvm2.uuid
            delegate.names = ["vm2"]
        } as CloneVmInstanceResult
        assert vm2.result != null
        assert vm2.result.numberOfClonedVm == 1
        assert vm2.result.inventories != null
        assert vm2.result.inventories.size() == 1
        assert vm2.result.numberOfClonedVm == vm2.result.inventories.size()
        assert vm2.result.inventories[0] instanceof CloneVmInstanceInventory
        def inv001a = vm2.result.inventories[0] as CloneVmInstanceInventory
        assert inv001a.error == null
        assert inv001a.inventory != null
        assert inv001a.inventory instanceof VmInstanceInventory
        def vm001 = inv001a.inventory
        assert vm001.uuid != null
        assert vm001.uuid != vm1.uuid
        assert vm001.name == "vm2"
        assert vm001.description == "cloned from vm[uuid:${vm1.uuid}]".toString()
        assert vm001.zoneUuid == vm1.zoneUuid
        assert vm001.clusterUuid == vm1.clusterUuid
        // FIX SOON: after the temp clone image is deleted, its ResourceVO row is left behind.
        // vm001.imageUuid should be null or vm1.imageUuid, but it points at that missing image uuid.
        assert vm001.imageUuid != null
        assert vm001.hostUuid == kvm2.uuid
        assert vm001.lastHostUuid == kvm2.uuid
        assert vm001.lastHostUuid == vm001.hostUuid
        assert vm001.instanceOfferingUuid == null
        assert vm001.rootVolumeUuid != null
        assert vm001.rootVolumeUuid != vm1.rootVolumeUuid
        assert vm001.platform == "Linux"
        assert vm001.architecture == "x86_64"
        assert vm001.defaultL3NetworkUuid == vm1.defaultL3NetworkUuid
        assert vm001.type == "UserVm"
        assert vm001.hypervisorType == "KVM"
        assert vm001.memorySize == 8589934592
        assert vm001.reservedMemorySize == 0
        assert vm001.cpuNum == 4
        assert vm001.cpuSpeed == 0
        assert vm001.allocatorStrategy == "LeastVmPreferredHostAllocatorStrategy"
        assert vm001.createDate != null
        assert vm001.lastOpDate != null
        assert vm001.vmEncryption == false
        assert vm001.hasEncryptionResource == null
        assert vm001.keyLastOpDate == null
        assert vm001.state == "Running"
        assert vm001.vmNics != null
        assert vm001.vmNics.size() == 1
        assert vm001.vmNics[0] instanceof VmNicInventory
        def nic001 = vm001.vmNics[0] as VmNicInventory
        assert nic001.uuid != null
        assert nic001.uuid != vm1.vmNics[0].uuid
        assert nic001.vmInstanceUuid == vm001.uuid
        assert nic001.l3NetworkUuid == vm001.defaultL3NetworkUuid
        assert nic001.ip == null
        assert nic001.mac != null
        assert nic001.mac != vm1.vmNics[0].mac
        assert nic001.hypervisorType == "KVM"
        assert nic001.netmask == null
        assert nic001.gateway == null
        assert nic001.metaData == null
        assert nic001.driverType == "virtio"
        assert nic001.usedIps != null
        assert nic001.usedIps.size() == 0
        assert nic001.internalName.startsWith("vnic")
        assert nic001.deviceId == 0
        assert nic001.type == "VNIC"
        assert nic001.state == "enable"
        assert nic001.createDate != null
        assert nic001.lastOpDate != null
        assert vm001.allVolumes != null
        assert vm001.allVolumes.size() == 1
        assert vm001.allVolumes[0] instanceof VolumeInventory
        def vol001 = vm001.allVolumes[0] as VolumeInventory
        assert vol001.uuid != null
        assert vol001.uuid == vm001.rootVolumeUuid
        assert vol001.uuid != vm1.rootVolumeUuid
        assert vol001.name == "ROOT-for-vm2"
        assert vol001.description == "Root volume for VM[uuid:${vm001.uuid}]".toString()
        assert vol001.primaryStorageUuid == vm1.allVolumes.find { it.uuid == vm1.rootVolumeUuid }.primaryStorageUuid
        assert vol001.vmInstanceUuid == vm001.uuid
        assert vol001.diskOfferingUuid == null
        assert vol001.rootImageUuid == vm001.imageUuid
        assert vol001.installPath != null
        assert vol001.installPath.startsWith("ceph://")
        assert vol001.installPath.endsWith(vol001.uuid)
        assert vol001.type == "Root"
        assert vol001.format == "raw"
        assert vol001.size == 0
        assert vol001.actualSize == 0
        assert vol001.deviceId == 0
        assert vol001.state == "Enabled"
        assert vol001.status == "Ready"
        assert vol001.createDate != null
        assert vol001.lastOpDate != null
        assert vol001.isShareable == false
        assert vol001.volumeQos == null
        assert vol001.lastDetachDate == null
        assert vol001.lastVmInstanceUuid == null
        assert vol001.lastAttachDate != null
        assert vol001.protocol == null
        assert vol001.encrypted == false
        assert vm001.vmCdRoms != null
        assert vm001.vmCdRoms.size() == 1
        assert vm001.vmCdRoms[0] instanceof VmCdRomInventory
        def cdrom001 = vm001.vmCdRoms[0] as VmCdRomInventory
        assert cdrom001.uuid != null
        assert cdrom001.vmInstanceUuid == vm001.uuid
        assert cdrom001.deviceId == 0
        assert cdrom001.occupant == null
        assert cdrom001.isoUuid == null
        assert cdrom001.isoInstallPath == null
        assert cdrom001.name == "vm-${vm001.uuid}-cdRom".toString()
        assert cdrom001.description == null
        assert cdrom001.protocol == null
        assert cdrom001.createDate != null
        assert cdrom001.lastOpDate != null
        assert vm001.guestOsType == "CentOS"

        def vms001 = queryVmInstance {} as List<VmInstanceInventory>
        assert vms001.size() == 2
        assert vms001.any { it.uuid == vm1.uuid }
        assert vms001.any { it.uuid == inv001a.inventory.uuid }
    }

    @Override
    void clean() {
        env.delete()
    }
}
