package org.zstack.test.integration.kvm.vm.cdrom

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.core.db.SimpleQuery
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.cdrom.VmCdRomVO
import org.zstack.header.vm.cdrom.VmCdRomVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.stream.Collectors

/**
 * Created by lining on 2018/02/10.
 */
class VmCdRomBasicCase extends SubCase {
    EnvSpec env
    VmCdRomInventory deletedCdrom

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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "iso_0"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_1"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_2"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_3"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("iso_1")
                systemTags = [
                        "${VmSystemTags.CD_ROM_LIST_TOKEN}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.NONE_CDROM}".toString()
                ]
            }
        }

    }

    @Override
    void test() {
        env.create {
            testVmCdRoms()
            testDeleteVmCdRom()
            testCreateVmCdRom()
            testUpdateVmCdRom()
            testSetVmInstanceDefaultCdRom()
            testAttachIsoToVm()
            testDetachIso()

            testCreateVmWithCdRoms()
            testCreateVmWithoutCdRom()
        }
    }

    void testVmCdRoms() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        List<VmCdRomInventory> cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
        }
        assert 3 == cdRoms.size()

        List<Integer> deviceIds = cdRoms.stream().map{VmCdRomInventory inv -> inv.getDeviceId()}.collect(Collectors.toList())
        assert [0, 1, 2].containsAll(deviceIds)

        for (VmCdRomInventory vmCdRomInventory : cdRoms) {
            assert vmCdRomInventory.uuid != null
            assert vmCdRomInventory.name != null

            if (vmCdRomInventory.deviceId == 0) {
                assert vmCdRomInventory.isoUuid == vm.imageUuid
                assert vmCdRomInventory.isoInstallPath != null
                continue
            }

            assert vmCdRomInventory.isoInstallPath == null
            assert vmCdRomInventory.isoUuid  == null
            assert vmCdRomInventory.occupant  == null
        }
    }

    void testDeleteVmCdRom() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        List<VmCdRomInventory> cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
        }
        assert cdRoms.size() >=0
        int cdRomNum = cdRoms.size()

        deletedCdrom = cdRoms.get(0)
        String cdRomUuid = deletedCdrom.uuid
        String deviceId = deletedCdrom.deviceId

        // vm is Runining
        expect([AssertionError.class]) {
            deleteVmCdRom {
                uuid = cdRomUuid
            }
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        deleteVmCdRom {
            uuid = cdRomUuid
        }

        cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
        }
        assert cdRomNum -1 == cdRoms.size()

        List<Integer> deviceIds = cdRoms.stream().map{VmCdRomInventory inv -> inv.getDeviceId()}.collect(Collectors.toList())
        List<Integer> allDeviceIds = [0, 1, 2]
        allDeviceIds.removeAll(deviceIds)
        assert allDeviceIds.size() == 1

        for (VmCdRomInventory inventory : cdRoms) {
            assert inventory.uuid != cdRomUuid
            assert inventory.deviceId != deviceId
        }
    }

    void testCreateVmCdRom() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        VmCdRomInventory cdRomInventory = createVmCdRom {
            name = "newCdRom"
            description = "desc"
            vmInstanceUuid = vm.uuid
        }
        assert cdRomInventory.deviceId == deletedCdrom.deviceId
        assert cdRomInventory.name == "newCdRom"
        assert cdRomInventory.description == "desc"
        assert cdRomInventory.vmInstanceUuid == vm.uuid
        assert cdRomInventory.occupant == null
        assert cdRomInventory.isoUuid == null
        assert cdRomInventory.isoInstallPath == null
    }

    void testUpdateVmCdRom() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        List<VmCdRomInventory> cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
            limit = 1
        }

        VmCdRomInventory target = cdRoms.get(0)

        VmCdRomInventory inventory = updateVmCdRom {
            uuid = target.uuid
            name = "newNameCd-1"
            description = "new desc"
        }
        assert inventory.name == "newNameCd-1"
        assert inventory.description == "new desc"
        assert inventory.uuid == target.uuid
        assert inventory.deviceId == target.deviceId
        assert inventory.occupant == target.occupant
        assert inventory.isoInstallPath == target.isoInstallPath
        assert inventory.isoUuid == target.isoUuid
        assert inventory.vmInstanceUuid == target.vmInstanceUuid
    }

    void testSetVmInstanceDefaultCdRom() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        List<VmCdRomInventory> cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
        }
        assert 3 == cdRoms.size()

        VmCdRomInventory target = cdRoms.find { VmCdRomInventory cdRom -> cdRom.deviceId == 1}
        VmCdRomInventory source = cdRoms.find { VmCdRomInventory cdRom -> cdRom.deviceId == 0}

        setVmInstanceDefaultCdRom {
            uuid = target.getUuid()
            vmInstanceUuid = vm.getUuid()
        }

        cdRoms = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.getUuid()}".toString()
            ]
        }

        List<Integer> deviceIds = cdRoms.stream().map{VmCdRomInventory inv -> inv.getDeviceId()}.collect(Collectors.toList())
        assert [0, 1, 2].containsAll(deviceIds)

        for (VmCdRomInventory inventory : cdRoms) {
            if (inventory.uuid == target.uuid) {
                assert inventory.deviceId == 0
                continue
            }

            if (inventory.uuid == source.uuid) {
                assert inventory.deviceId == 1
                continue
            }

            assert inventory.deviceId == 2
        }
    }

    void testAttachIsoToVm() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        ImageInventory iso = env.inventoryByName("iso_2")

        VmCdRomVO targetCdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vm.uuid)
                .isNull(VmCdRomVO_.isoUuid)
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .limit(1)
                .find()
        assert targetCdRomVO != null

        startVmInstance {
            uuid = vm.uuid
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        VmCdRomVO cdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.uuid, targetCdRomVO.uuid)
                .find()

        assert cdRomVO.vmInstanceUuid == targetCdRomVO.vmInstanceUuid
        assert cdRomVO.deviceId == targetCdRomVO.deviceId
        assert cdRomVO.name == targetCdRomVO.name
        assert cdRomVO.description == targetCdRomVO.description
        assert cdRomVO.isoInstallPath != null
        assert cdRomVO.isoUuid == iso.uuid
        assert cdRomVO.occupant == "ISO"

        rebootVmInstance {
            delegate.uuid = vm.uuid
        }

        cdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.uuid, targetCdRomVO.uuid)
                .find()

        assert cdRomVO.vmInstanceUuid == targetCdRomVO.vmInstanceUuid
        assert cdRomVO.deviceId == targetCdRomVO.deviceId
        assert cdRomVO.name == targetCdRomVO.name
        assert cdRomVO.description == targetCdRomVO.description
        assert cdRomVO.isoInstallPath != null
        assert cdRomVO.isoUuid == iso.uuid
        assert cdRomVO.occupant == "ISO"
    }

    void testDetachIso() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        ImageInventory iso = env.inventoryByName("iso_2")

        VmCdRomVO cdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vm.uuid)
                .eq(VmCdRomVO_.isoUuid, iso.uuid)
                .find()

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        VmCdRomVO afterCdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.uuid, cdRomVO.uuid)
                .find()
        assert afterCdRomVO.isoUuid == null
        assert afterCdRomVO.isoInstallPath == null
        assert afterCdRomVO.deviceId == cdRomVO.deviceId
        assert afterCdRomVO.vmInstanceUuid == cdRomVO.vmInstanceUuid

    }

    void testCreateVmWithCdRoms() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        ImageInventory iso = env.inventoryByName("iso_2")
        ImageInventory image = env.inventoryByName("image")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")

        VmInstanceInventory newVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            rootDiskOfferingUuid = diskOfferingInventory.uuid
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}".toString()
            ]
        }
        assert 3 == newVm.vmCdRoms.size()


        expect(AssertionError.class) {
            createVmInstance {
                name = "vmWithDuplicateIso"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = iso.uuid
                l3NetworkUuids = [vm.defaultL3NetworkUuid]
                rootDiskOfferingUuid = diskOfferingInventory.uuid
                systemTags = [
                        "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::${iso.uuid}::${VmInstanceConstant.EMPTY_CDROM}".toString()
                ]
            }
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "vmWithErrorISO"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = iso.uuid
                l3NetworkUuids = [vm.defaultL3NetworkUuid]
                rootDiskOfferingUuid = diskOfferingInventory.uuid
                systemTags = [
                        "${VmSystemTags.CD_ROM_LIST_TOKEN}::errorImageUuid::${iso.uuid}::${VmInstanceConstant.EMPTY_CDROM}".toString()
                ]
            }
        }


        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 1 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                assert cdRomTO.isEmpty()
                assert null == cdRomTO.imageUuid
                assert i == cdRomTO.deviceId
                assert null == cdRomTO.path
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.hd.toString() == cmd.getBootDev().get(0)

            return rsp
        }

        VmInstanceInventory oneEmptyCdRomVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = image.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::empty::none::none".toString()
            ]
        }
        assert oneEmptyCdRomVm.getVmCdRoms().size() == 1
        assert cmd != null


        cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 3 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                assert cdRomTO.isEmpty()
                assert null == cdRomTO.imageUuid
                assert i == cdRomTO.deviceId
                assert null == cdRomTO.path
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.hd.toString() == cmd.getBootDev().get(0)

            return rsp
        }
        VmInstanceInventory threeEmptyCdRomVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = image.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::empty::empty::empty".toString()
            ]
        }
        assert threeEmptyCdRomVm.getVmCdRoms().size() == 3
        assert cmd != null


        cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 3 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                if (cdRomTO.deviceId == 0) {
                    assert !cdRomTO.isEmpty()
                    assert iso.uuid == cdRomTO.imageUuid
                    assert null != cdRomTO.path
                } else {
                    assert cdRomTO.isEmpty()
                    assert null == cdRomTO.imageUuid
                    assert null == cdRomTO.path
                }

                assert i == cdRomTO.deviceId
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.hd.toString() == cmd.getBootDev().get(0)

            return rsp
        }
        VmInstanceInventory twoEmptyCdRomVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = image.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::empty::empty".toString()
            ]
        }
        assert twoEmptyCdRomVm.getVmCdRoms().size() == 3
        assert cmd != null


        cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 2 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                if (cdRomTO.deviceId == 0) {
                    assert !cdRomTO.isEmpty()
                    assert iso.uuid == cdRomTO.imageUuid
                    assert null != cdRomTO.path
                } else {
                    assert cdRomTO.isEmpty()
                    assert null == cdRomTO.imageUuid
                    assert null == cdRomTO.path
                }

                assert i == cdRomTO.deviceId
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.hd.toString() == cmd.getBootDev().get(0)

            return rsp
        }

        oneEmptyCdRomVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = image.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::empty::none".toString()
            ]
        }
        assert oneEmptyCdRomVm.getVmCdRoms().size() == 2
        assert cmd != null


        cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 1 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                assert !cdRomTO.isEmpty()
                assert iso.uuid == cdRomTO.imageUuid
                assert null != cdRomTO.path
                assert i == cdRomTO.deviceId
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.hd.toString() == cmd.getBootDev().get(0)

            return rsp
        }

        VmInstanceInventory oneIsoCdRomVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = image.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::none::none".toString()
            ]
        }
        assert oneIsoCdRomVm.getVmCdRoms().size() == 1

        
        cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert 1 == cmd.getCdRoms().size()

            int i = 0
            for ( KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
                assert !cdRomTO.isEmpty()
                assert iso.uuid == cdRomTO.imageUuid
                assert null != cdRomTO.path
                assert i == cdRomTO.deviceId
                i ++
            }

            assert 1 == cmd.getBootDev().size()
            assert KVMAgentCommands.BootDev.cdrom.toString() == cmd.getBootDev().get(0)

            return rsp
        }

        createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            rootDiskOfferingUuid = diskOfferingInventory.uuid
        }
        assert null != cmd
    }

    void testCreateVmWithoutCdRom() {
        VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.updateValue(0)

        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("image")
        ImageInventory iso = env.inventoryByName("iso_0")

        env.cleanAfterSimulatorHandlers()

        VmInstanceInventory newVm = createVmInstance {
            name = "new-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        assert 0 == newVm.vmCdRoms.size()

        newVm = createVmInstance {
            name = "new-vm-with-iso"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        assert 1 == newVm.vmCdRoms.size()
    }
}