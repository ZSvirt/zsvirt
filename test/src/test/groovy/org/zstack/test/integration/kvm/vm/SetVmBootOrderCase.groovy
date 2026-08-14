package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.VmBootDevice
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.GetVmBootOrderResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.TagUtils
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

class SetVmBootOrderCase extends SubCase {
    EnvSpec env
    KVMAgentCommands.StartVmCmd startVmCmd
    VmInstanceInventory vm
    ImageInventory iso_0, iso_1
    L3NetworkInventory l3_1, l3_2
    DatabaseFacade dbf

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
                memory = SizeUnit.GIGABYTE.toByte(8)
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
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "iso_0"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_1"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
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
                        name = "l3_1"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                    l3Network {
                        name = "l3_2"
                        ip {
                            startIp = "192.168.100.101"
                            endIp = "192.168.100.200"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3_1", "l3_2")
                useDefaultL3Network("l3_1")
                systemTags = [
                        "${VmSystemTags.CD_ROM_LIST_TOKEN}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.NONE_CDROM}".toString()
                ]
            }
        }
    }

    @Override
    void test() {
        env.create {
            simulatorEnv()
            prepareEnv()
            testSetVmBootFromHardDisk()
            testSetVmBootFromCdRom()
            testSetVmBootFromNetWork()
            testSetVmBootFromBootOrderOnce()
            testSetVmBootFromBootOrderOnceTemporarily()
            testBootOrderWhenVmDetachDevice()
            testBootOrderWhenVmAttachDevice()
            testSetBootOrderWhenCreateVm()
        }
    }

    void simulatorEnv() {
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startVmCmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }
    }

    void prepareEnv() {
        dbf = bean(DatabaseFacade.class)
        vm = env.inventoryByName("vm")
        l3_1 = env.inventoryByName("l3_1")
        l3_2 = env.inventoryByName("l3_2")
        iso_0 = env.inventoryByName("iso_0")
        iso_1 = env.inventoryByName("iso_1")
        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso_0.uuid
        }
        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso_1.uuid
        }
    }

    void testSetVmBootFromHardDisk() {
        int bootOrderNum = 0

        expect(AssertionError.class) {
            setVmBootOrder {
                uuid = vm.uuid
                bootOrder = asList("invalid boot order")
            }
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }

        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()

        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum

        int defaultOrderBootNum = ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class)
            if (nicVO.getL3NetworkUuid() == l3_1.uuid) {
                assert nic.getBootOrder() == defaultOrderBootNum
            } else {
                assert nic.getBootOrder() == ++bootOrderNum
            }
        })
    }

    void testSetVmBootFromCdRom() {
        int bootOrderNum = 0

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString(), VmBootDevice.Network.toString())
        }
        
        assert Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, vm.uuid)
                .eq(SystemTagVO_.inherent, false)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(VmSystemTags.BOOT_ORDER.tagFormat))
                .count() == 1
        assert !Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, vm.uuid)
                .eq(SystemTagVO_.inherent, true)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(VmSystemTags.BOOT_ORDER.tagFormat))
                .isExists()

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()

        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum

        int defaultOrderBootNum = ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class)
            if (nicVO.getL3NetworkUuid() == l3_1.uuid) {
                assert nic.getBootOrder() == defaultOrderBootNum
            } else {
                assert nic.getBootOrder() == ++bootOrderNum
            }
        })
    }

    void testSetVmBootFromNetWork() {
        int bootOrderNum = 0

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.Network.toString(), VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString())
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.Network.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(2) == VmBootDevice.CdRom.toString()

        int defaultOrderBootNum = ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class)
            if (nicVO.getL3NetworkUuid() == l3_1.uuid) {
                assert nic.getBootOrder() == defaultOrderBootNum
            } else {
                assert nic.getBootOrder() == ++bootOrderNum
            }
        })
        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum
    }

    void testSetVmBootFromBootOrderOnce() {
        int bootOrderNum = 0

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["bootOrderOnce::true"]
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert "true" == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert "CdRom,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        rebootVmInstance {
            uuid = vm.uuid
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }

        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()
        assert null == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert null == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            assert nic.getBootOrder() == 0
        })

        rebootVmInstance {
            uuid = vm.uuid
        }

        bootOrderNum = 0
        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == 0
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == 0
        startVmCmd.getNics().forEach({ nic ->
            assert nic.getBootOrder() == 0
        })

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["bootOrderOnce::true"]
        }

        stopVmInstance {
            uuid = vm.uuid
            type = "cold"
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }

        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert "true" == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert "CdRom,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        startVmInstance {
            uuid = vm.uuid
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }

        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()
        assert null == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert null == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        //test cdromBootOnce in case of upgrade
        SystemTagInventory tag1 = createSystemTag {
            resourceType = VmInstanceVO.class.simpleName
            resourceUuid = vm.uuid
            tag = "cdromBootOnce::true"
        }
        SystemTagInventory tag2 = createSystemTag {
            resourceType = VmInstanceVO.class.simpleName
            resourceUuid = vm.uuid
            tag = "bootOrder::CdRom,HardDisk"
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()

        assert "true" == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)
        assert "CdRom,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        rebootVmInstance {
            uuid = vm.uuid
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()

        assert null == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)
        assert null == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        tag1 = createSystemTag {
            resourceType = VmInstanceVO.class.simpleName
            resourceUuid = vm.uuid
            tag = "cdromBootOnce::true"
        }
        tag2 = createSystemTag {
            resourceType = VmInstanceVO.class.simpleName
            resourceUuid = vm.uuid
            tag = "bootOrder::CdRom,HardDisk"
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.Network.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["bootOrderOnce::true"]
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.Network.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()

        assert null == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)
        assert "true" == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert "Network,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)
    }

    void testSetVmBootFromBootOrderOnceTemporarily() {
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["bootOrderOnce::true"]
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert null == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert "CdRom,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        rebootVmInstance {
            uuid = vm.uuid
        }

        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert null == VmSystemTags.BOOT_ORDER_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
        assert "CdRom,HardDisk" == VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(vm.uuid, VmSystemTags.BOOT_ORDER_TOKEN)

        int bootOrderNum = 0
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            assert nic.getBootOrder() == 0
        })
    }

    void testBootOrderWhenVmDetachDevice() {
        int bootOrderNum = 0

        VmNicInventory nicInventory = queryVmNic {
            conditions = ["l3Network.name=l3_1", "vmInstance.uuid=${vm.uuid}"]
        }[0]

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso_1.uuid
        }
        detachL3NetworkFromVm {
            vmNicUuid = nicInventory.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()

        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getNics().get(0).getBootOrder() == ++bootOrderNum
    }

    void testBootOrderWhenVmAttachDevice() {
        int bootOrderNum = 0

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso_1.uuid
        }

        attachL3NetworkToVm {
            l3NetworkUuid = l3_1.uuid
            vmInstanceUuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 3
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
        assert res.orders.get(2) == VmBootDevice.Network.toString()

        assert startVmCmd.getRootVolume().getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(0).getBootOrder() == ++bootOrderNum
        assert startVmCmd.getCdRoms().get(1).getBootOrder() == ++bootOrderNum

        int defaultOrderBootNum = ++bootOrderNum
        startVmCmd.getNics().forEach({ nic ->
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class)
            if (nicVO.getL3NetworkUuid() == l3_2.uuid) {
                assert nic.getBootOrder() == defaultOrderBootNum
            } else {
                assert nic.getBootOrder() == ++bootOrderNum
            }
        })
    }

    /**
     * ZSTAC-38152
     */
    void testSetBootOrderWhenCreateVm() {
        def l3_1 = env.inventoryByName("l3_1") as L3NetworkInventory
        
        def vm2 = createVmInstance {
            delegate.name = "vm2"
            delegate.imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
            delegate.instanceOfferingUuid = (
                    env.inventoryByName("instanceOffering") as InstanceOfferingInventory).uuid
            delegate.l3NetworkUuids = [l3_1.uuid]
            delegate.systemTags = [
                    "${VmSystemTags.BOOT_ORDER_TOKEN}::${VmBootDevice.Network.toString()}".toString(),
                    "${VmSystemTags.BOOT_ORDER_ONCE_TOKEN}::true".toString()]
        } as VmInstanceInventory
        
        def tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm2.uuid}".toString()]
        } as List<SystemTagInventory>
        
        assert tags.count { tag -> VmSystemTags.BOOT_ORDER.isMatch(tag.tag) } == 0
        assert tags.count { tag -> VmSystemTags.BOOT_ORDER_ONCE.isMatch(tag.tag) } == 0
        
        destroyVmInstance {
            delegate.uuid = vm2.uuid
        }

        def vm3 = createVmInstance {
            delegate.name = "vm3"
            delegate.imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
            delegate.instanceOfferingUuid = (
                    env.inventoryByName("instanceOffering") as InstanceOfferingInventory).uuid
            delegate.l3NetworkUuids = [l3_1.uuid]
            delegate.systemTags = ["${VmSystemTags.BOOT_ORDER_TOKEN}::${VmBootDevice.Network.toString()}".toString()]
        } as VmInstanceInventory

        tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm3.uuid}".toString()]
        } as List<SystemTagInventory>

        assert tags.count { tag -> VmSystemTags.BOOT_ORDER.isMatch(tag.tag) && !tag.inherent } == 1
        assert tags.count { tag -> VmSystemTags.BOOT_ORDER.isMatch(tag.tag) && tag.inherent } == 0
        
        setVmBootOrder {
            delegate.uuid = vm3.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }
        tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm3.uuid}".toString()]
        } as List<SystemTagInventory>
        
        assert tags.count { tag -> VmSystemTags.BOOT_ORDER.isMatch(tag.tag) && !tag.inherent } == 1
        assert tags.count { tag -> VmSystemTags.BOOT_ORDER.isMatch(tag.tag) && tag.inherent } == 0
    }
}
