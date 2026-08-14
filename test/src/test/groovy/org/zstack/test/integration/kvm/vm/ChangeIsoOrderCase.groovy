package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.IsoOperator
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.image.ImageConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2018/02/22.
 */
class ChangeIsoOrderCase extends SubCase {
    EnvSpec env

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
        }

    }

    @Override
    void test() {
        env.create {
            testAttach1Iso()
            testAttach2Iso()
            testAttach3Iso()
        }
    }

    void testAttach1Iso() {
        VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.updateValue(3)

        ImageInventory iso0 = env.inventoryByName("iso_0")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory newVm = createVmInstance {
            name = "new-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso0.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

        updateVmInstance {
            uuid = newVm.uuid
            systemTags = [VmSystemTags.ISO.instantiateTag([(VmSystemTags.ISO_TOKEN): iso0.uuid, (VmSystemTags.ISO_DEVICEID_TOKEN) : 0])]
        }
        checkIsoSystemTag(newVm.uuid, iso0.uuid, 0)
        checkVmIsoSystemTags(newVm.uuid)
        checkVmIsoNum(newVm.uuid, 1)

        rebootVmInstance {
            uuid = newVm.uuid
        }
    }

    void testAttach2Iso() {
        ImageInventory iso0 = env.inventoryByName("iso_0")
        ImageInventory iso1 = env.inventoryByName("iso_1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory newVm = createVmInstance {
            name = "new-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso0.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        attachIso(newVm.uuid, iso1.uuid, 1)

        updateVmInstance {
            uuid = newVm.uuid
            systemTags = [VmSystemTags.ISO.instantiateTag([(VmSystemTags.ISO_TOKEN): iso1.uuid, (VmSystemTags.ISO_DEVICEID_TOKEN) : 0])]
        }
        checkIsoSystemTag(newVm.uuid, iso1.uuid, 0)
        checkIsoSystemTag(newVm.uuid, iso0.uuid, 1)
        checkVmIsoSystemTags(newVm.uuid)
        checkVmIsoNum(newVm.uuid, 2)

        rebootVmInstance {
            uuid = newVm.uuid
        }
    }

    void testAttach3Iso() {
        ImageInventory iso0 = env.inventoryByName("iso_0")
        ImageInventory iso1 = env.inventoryByName("iso_1")
        ImageInventory iso2 = env.inventoryByName("iso_2")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory newVm = createVmInstance {
            name = "new-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso0.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        attachIso(newVm.uuid, iso1.uuid, 1)
        attachIso(newVm.uuid, iso2.uuid, 2)

        updateVmInstance {
            uuid = newVm.uuid
            systemTags = [VmSystemTags.ISO.instantiateTag([(VmSystemTags.ISO_TOKEN): iso2.uuid, (VmSystemTags.ISO_DEVICEID_TOKEN) : 0])]
        }
        checkIsoSystemTag(newVm.uuid, iso2.uuid, 0)
        checkVmIsoSystemTags(newVm.uuid)
        checkVmIsoNum(newVm.uuid, 3)

        rebootVmInstance {
            uuid = newVm.uuid
        }
    }

    void attachIso(String vmUuid, String iso, int expectIsoDeviceId) {
        KVMAgentCommands.AttachIsoCmd cmd
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.AttachIsoCmd.class)
            assert expectIsoDeviceId == cmd.iso.deviceId
            assert iso == cmd.iso.imageUuid
            assert null != cmd.iso.path

            return rsp
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vmUuid
            isoUuid = iso
        }

        assert null != cmd
        checkIsoSystemTag(vmUuid, iso, expectIsoDeviceId)

    }

    void checkIsoSystemTag(String vmUuid, String isoUuid, int expectIsoDeviceId) {
        assert expectIsoDeviceId == IsoOperator.getIsoDeviceId(vmUuid, isoUuid)
    }

    void checkVmIsoNum(String vmUuid, int expectIsoNum) {
        assert expectIsoNum == IsoOperator.getIsoUuidByVmUuid(vmUuid).size()
    }

    void checkVmIsoSystemTags(String vmUuid) {
        if (!IsoOperator.isIsoAttachedToVm(vmUuid)) {
            return
        }

        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(vmUuid)

        int max = VmGlobalConfig.MAXIMUM_CD_ROM_NUM.value(Integer.class)
        assert isoUuids.size() <= max

        List<String> repeatedIsoUuids = []

        assert repeatedIsoUuids.isEmpty()
    }

}