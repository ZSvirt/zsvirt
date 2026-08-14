package org.zstack.test.integration.storage.systemtag

import org.springframework.http.HttpEntity
import org.zstack.header.volume.VolumeConstant
import org.zstack.sdk.*
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by miao on 17-7-12.
 */
class CreateSystemTagForAPICreateDataVolumeFromVolumeTemplateMsgCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        // CephStorageOneVmEnv
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
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
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
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(300)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("ceph-bk")
            }

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
                    url = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "test-vm"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useImage("image")

            }

        }
    }

    @Override
    void test() {
        env.create {
            testCreateSystemTagForAPICreateDataVolumeFromVolumeTemplateMsg()
        }

    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateSystemTagForAPICreateDataVolumeFromVolumeTemplateMsg() {
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        VolumeInventory vol = vm.getAllVolumes().find {
            it -> it.uuid != vm.getRootVolumeUuid()
        }

        // create volume template
        stopVmInstance {
            uuid = vm.uuid
            sessionId = loginAsAdmin().uuid
        }

        ImageInventory img = createDataVolumeTemplateFromVolume {
            name = "template"
            volumeUuid = vol.uuid
            sessionId = loginAsAdmin().uuid
        } as ImageInventory

        assert VolumeConstant.VOLUME_FORMAT_RAW == img.getFormat()

        env.hijackSimulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { rsp, HttpEntity<String> e ->
            rsp.actualSize = null
            return rsp
        }

        // create volume from template with system tags
        VolumeInventory dataVolume = createDataVolumeFromVolumeTemplate {
            imageUuid = img.uuid
            name = "new"
            primaryStorageUuid = ps.uuid
            systemTags = ["capability::virtio-scsi".toString()]
        } as VolumeInventory

        // check tags
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${dataVolume.getUuid()}".toString()]
        } as List<SystemTagInventory>

        assert tags.size() == 2
        assert tags.collect{it.getTag()}.containsAll(["capability::virtio-scsi".toString(), "notSupportActualSize::true".toString()])

        changeVolumeState {
            uuid = dataVolume.uuid
            stateEvent = "disable"
        }

        deleteDataVolume {
            uuid = dataVolume.uuid
        }

        expungeDataVolume {
            uuid = dataVolume.uuid
        }
    }
}
