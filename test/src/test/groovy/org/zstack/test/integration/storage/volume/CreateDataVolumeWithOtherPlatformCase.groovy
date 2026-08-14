package org.zstack.test.integration.storage.volume

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImagePlatform
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class CreateDataVolumeWithOtherPlatformCase extends SubCase {
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
            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                availableCapacity = SizeUnit.GIGABYTE.toByte(300)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image1"
                    url = "http://zstack.org/download/test2.qcow2"
                    platform = ImagePlatform.Other.toString()
                    virtio = false
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    kvm {
                        name = "host"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(300)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]

                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("ceph-bk")
            }
        }
    }

    @Override
    void test() {
        env.create {
            TestCreateDataVolumeWithOtherPlatform()
        }
    }

    void TestCreateDataVolumeWithOtherPlatform() {
        ImageInventory image1 = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")

        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        def vm1 = createVmInstance {
            name = "vm1"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = image1.uuid
            l3NetworkUuids = [pubL3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(20),
                ]
            ]
        } as VmInstanceInventory

        assert cmd.getRootVolume().useVirtio == false
        assert cmd.getRootVolume().useVirtioSCSI == false
        cmd.getDataVolumes().forEach({ dataVolume ->
            assert dataVolume.useVirtio == false
            assert dataVolume.useVirtio == false
        })

        //attach the second dataVolume
        stopVmInstance {
            uuid = vm1.uuid
        }
        def volume1 = createDataVolume {
            name = "volume1"
            diskSize = gb(20)
            primaryStorageUuid = ps.uuid
            systemTags = ["capability::virtio-scsi".toString()]
        } as VolumeInventory
        attachDataVolumeToVm {
            vmInstanceUuid = vm1.uuid
            volumeUuid = volume1.uuid
        }
        startVmInstance {
            uuid = vm1.uuid
        }
        assert cmd.getRootVolume().useVirtio == false
        assert cmd.getRootVolume().useVirtioSCSI == false
        cmd.getDataVolumes().forEach({ dataVolume ->
            assert dataVolume.useVirtio == false
            assert dataVolume.useVirtio == false
        })

        //attach the third dataVolume
        stopVmInstance {
            uuid = vm1.uuid
        }
        def volume2 = createDataVolume {
            name = "volume2"
            diskSize = gb(20)
        } as VolumeInventory
        attachDataVolumeToVm {
            vmInstanceUuid = vm1.uuid
            volumeUuid = volume2.uuid
        }
        expectError {
            startVmInstance {
                uuid = vm1.uuid
            }
        }

        //attach the second cdrom
        detachDataVolumeFromVm {
            uuid = volume2.uuid
            vmUuid = vm1.uuid
        }
        createVmCdRom {
            name = "newCdRom"
            description = "desc"
            vmInstanceUuid = vm1.uuid
        }
        expectError {
            startVmInstance {
                uuid = vm1.uuid
            }
        }

        detachDataVolumeFromVm {
            uuid = volume1.uuid
            vmUuid = vm1.uuid
        }
        startVmInstance {
            uuid = vm1.uuid
        }
        assert cmd.getRootVolume().useVirtio == false
        assert cmd.getRootVolume().useVirtioSCSI == false
        cmd.getDataVolumes().forEach({ dataVolume ->
            assert dataVolume.useVirtio == false
            assert dataVolume.useVirtio == false
        })
    }
}
