package org.zstack.test.integration.image

import org.zstack.compute.vm.IsoOperator
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2018/02/10.
 */
class DeleteIsoCase extends SubCase {
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
                cpu = 4
                memoryGB(8)
                useL3Networks("l3")
                useImage("iso_1")
                disk {
                    name = "disk1"
                    boot = true
                    sizeGB(20)
                    usePrimaryStorage("local")
                }
            }
        }

    }

    @Override
    void test() {
        env.create {
            prepare()
            testDeleteIso()
        }
    }

    void prepare() {
        def vm = queryVmInstance {conditions = ["name=vm"]}[0] as VmInstanceInventory
        def localPs  = env.inventoryByName("local") as PrimaryStorageInventory
        def volumes = vm.allVolumes.findAll {
            volume -> volume.name == "disk1"
        }
        assert volumes.size() == 1
        assert volumes[0].primaryStorageUuid == localPs.uuid
    }

    void testDeleteIso() {
        VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.updateValue(3)

        def image = env.inventoryByName("image") as ImageInventory
        def iso1 = env.inventoryByName("iso_1") as ImageInventory
        def iso2 = env.inventoryByName("iso_2") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def newVm = createVmInstance {
            name = "new-vm"
            cpuNum = 4
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    boot: true,
                    size: SizeUnit.GIGABYTE.toByte(20)
                ]
            ]
        } as VmInstanceInventory

        def newVm2 = createVmInstance {
            name = "new-vm"
            cpuNum = 4
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    boot: true,
                    size: SizeUnit.GIGABYTE.toByte(20)
                ]
            ]
        } as VmInstanceInventory

        attachIsoToVmInstance {
            vmInstanceUuid = newVm.uuid
            isoUuid = iso1.uuid
        }

        attachIsoToVmInstance {
            vmInstanceUuid = newVm2.uuid
            isoUuid = iso1.uuid
        }

        attachIsoToVmInstance {
            vmInstanceUuid = newVm.uuid
            isoUuid = iso2.uuid
        }

        deleteImage {
            uuid = iso1.uuid
        }

        deleteImage {
            uuid = iso2.uuid
        }

        assert 0 == IsoOperator.getIsoUuidByVmUuid(newVm.uuid).size()
        assert 0 == IsoOperator.getIsoUuidByVmUuid(newVm2.uuid).size()
    }
}