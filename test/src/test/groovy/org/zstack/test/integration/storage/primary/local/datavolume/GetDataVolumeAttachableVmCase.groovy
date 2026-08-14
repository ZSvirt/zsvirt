package org.zstack.test.integration.storage.primary.local.datavolume

import org.zstack.header.image.ImageConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * @Author: fubang
 * @Date: 2018/6/26
 */
class GetDataVolumeAttachableVmCase extends SubCase {
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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(5)
            }

            diskOffering {
                name = "50G"
                diskSize = SizeUnit.GIGABYTE.toByte(50)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image-root-volume"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image-data-volume"
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate
                    url = "http://zstack.org/download/test-volume.qcow2"
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
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(90)
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

                attachBackupStorage("sftp")
            }

            vm {
                name = "test-vm"
                useInstanceOffering("instanceOffering")
                useImage("image-root-volume")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetDataVolumeAttachableVm()
        }
    }

    void testGetDataVolumeAttachableVm() {
        def hostInventory = env.inventoryByName("kvm") as HostInventory
        def primaryStorageInventory = env.inventoryByName("local") as PrimaryStorageInventory
        def diskOfferingInventory = env.inventoryByName("50G") as DiskOfferingInventory
        def vmInstanceInventory = env.inventoryByName("test-vm") as VmInstanceInventory

        VolumeInventory instantiateVolume = createDataVolume {
            name = "instantiate-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageInventory.uuid
            systemTags = ["localStorage::hostUuid::${hostInventory.uuid}".toString()]
        }

        def vms = getDataVolumeAttachableVm {
            volumeUuid = instantiateVolume.uuid
        } as List<VmInstanceInventory>

        assert vms.size() == 1
        assert vms.get(0).uuid == vmInstanceInventory.uuid

        attachDataVolumeToVm {
            vmInstanceUuid = vmInstanceInventory.uuid
            volumeUuid = instantiateVolume.uuid
        }

        VolumeInventory notInstantiateVolume = createDataVolume {
            name = "not-instantiate-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
        }

        vms = getDataVolumeAttachableVm {
            volumeUuid = notInstantiateVolume.uuid
        } as List<VmInstanceInventory>

        assert vms.size() == 0
    }

}
