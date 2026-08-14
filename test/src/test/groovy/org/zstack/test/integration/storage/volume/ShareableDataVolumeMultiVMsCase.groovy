package org.zstack.test.integration.storage.volume

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by heathhose on 17-3-24.
 */
class ShareableDataVolumeMultiVMsCase extends SubCase{

    EnvSpec env
    VolumeInventory voi

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(5)
            }
            zone{
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
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(300)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                availableCapacity= SizeUnit.GIGABYTE.toByte(300)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm1"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }

            vm {
                name = "vm2"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }

    @Override
    void test() {
        env.create {
            createShareableDataVolume()
            attachSharebleDataVolumeToVms()
        }
    }

    void createShareableDataVolume(){
        String tag = VolumeSystemTags.SHAREABLE.getTagFormat();
        String tag2 = KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat();
        DiskOfferingInventory disk = env.inventoryByName("diskOffering")

        voi = createDataVolume {
            name = "share"
            diskOfferingUuid = disk.uuid
            systemTags = Arrays.asList(tag,tag2)
        }
    }
    void attachSharebleDataVolumeToVms(){
        VmInstanceInventory vmi1 = env.inventoryByName("vm1")
        VmInstanceInventory vmi2 = env.inventoryByName("vm2")

        KVMAgentCommands.AttachDataVolumeCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME){rsp,HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body,KVMAgentCommands.AttachDataVolumeCmd.class)
            return rsp
        }
        attachDataVolumeToVm {
            volumeUuid = voi.uuid
            vmInstanceUuid = vmi1.uuid
        }
        assert cmd != null
        assert cmd.vmUuid == vmi1.uuid

        cmd = null
        attachDataVolumeToVm {
            volumeUuid = voi.uuid
            vmInstanceUuid = vmi2.uuid
        }
        assert cmd != null
        assert cmd.vmUuid == vmi2.uuid

        detachDataVolumeFromVm {
            uuid = voi.uuid
            vmUuid = vmi1.uuid
        }
        detachDataVolumeFromVm {
            uuid = voi.uuid
            vmUuid = vmi2.uuid
        }

        boolean success1, success2
        def thread1 = Thread.start {
            attachDataVolumeToVm {
                volumeUuid = voi.uuid
                vmInstanceUuid = vmi1.uuid
            }
            success1 = true
        }

        def thread2 = Thread.start {
            attachDataVolumeToVm {
                volumeUuid = voi.uuid
                vmInstanceUuid = vmi1.uuid
            }
            success2 = true
        }

        [thread1, thread2].each { it.join() }
        assert success1 && success2
    }
    @Override
    void clean() {
        env.delete()
    }
}