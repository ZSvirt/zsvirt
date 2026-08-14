package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Create by lining at 2019/1/13
 */
class CephVmIsoCase extends SubCase {
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
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            zone{
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ps")
                    attachL2Network("l2")
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                cephPrimaryStorage {
                    name="ps"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }

                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name="bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "iso"
                    url = "http://zstack.org/download/vr.qcow2"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVmWithIso()
        }
    }

    private void testCreateVmWithIso() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        ImageInventory iso = env.inventoryByName("iso")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")

        KVMAgentCommands.StartVmCmd cmd
        String cmdString
        String isoInstallPath
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            cmdString = e.body
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOfferingInventory.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}".toString()
            ]
        }
        assert vm.getVmCdRoms().size() == 3
        assert cmd != null
        assert 3 == cmd.getCdRoms().size()
        assert null != cmdString
        assert cmdString.contains("monInfo")
        assert cmdString.contains("cdRoms")
        String cdRomConfigString = cmdString.substring(cmdString.indexOf("cdRoms"), cmdString.size())
        assert cdRomConfigString.contains("monInfo")

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
        }

    }

}
