package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.header.vm.VmCreationStrategy
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by kayo on 2018/2/5.
 */
class AttachDataVolumeCrossClusterCase extends SubCase {
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
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.20.0.1:/nfs_root"
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
                useCluster("cluster2")
                useHost("kvm3")
                useL3Networks("l3")
                cpu = 1
                memoryGB(1)
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }

    @Override
    void test() {
        env.create {
            testDataVolumeCrossClusterAttachedForStoppedVm()
            testDataVolumeCrossClusterAttachedForJustCreatedVm()
            testDataVolumeCrossClusterAttachedInMultiPs()
            testDataVolumeAttachedInMultiPs()
        }
    }

    void testDataVolumeCrossClusterAttachedForStoppedVm() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory local = env.inventoryByName("local")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        HostInventory host = env.inventoryByName("kvm")
        stopVmInstance {
            uuid = vm.uuid
        }

        VolumeInventory volume = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::" + host.uuid]
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction()
        action.sessionId = adminSession()
        action.vmInstanceUuid = vm.uuid
        action.volumeUuid = volume.uuid
        AttachDataVolumeToVmAction.Result ret = action.call()

        // assert ret.error.details.contains("attach volume to VM, no qualified cluster")
        assert ret.error != null
    }

    void testDataVolumeCrossClusterAttachedForJustCreatedVm() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory local = env.inventoryByName("local")
        HostInventory host = env.inventoryByName("kvm")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        HostInventory host4 = env.inventoryByName("kvm4")

        VmInstanceInventory vm = createVmInstance {
            name = "test-2"
            imageUuid = image.uuid
            cpuNum = 1
            memorySize = gb(1)
            l3NetworkUuids = [l3.uuid]
            hostUuid = host4.uuid
            strategy = VmCreationStrategy.JustCreate.toString()
        }

        VolumeInventory volume = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::" + host.uuid]
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction()
        action.sessionId = adminSession()
        action.vmInstanceUuid = vm.uuid
        action.volumeUuid = volume.uuid
        AttachDataVolumeToVmAction.Result ret = action.call()

        // assert ret.error.details.contains("attach volume to VM, no qualified cluster")
        assert ret.error != null
    }

    void testDataVolumeCrossClusterAttachedInMultiPs() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory local = env.inventoryByName("local")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        HostInventory host = env.inventoryByName("kvm")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ClusterInventory cluster = env.inventoryByName("cluster")
        HostInventory host4 = env.inventoryByName("kvm4")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = local.uuid
            clusterUuid = cluster2.uuid
        }

        VmInstanceInventory vm = createVmInstance {
            name = "test-3"
            imageUuid = image.uuid
            cpuNum = 1
            memorySize = gb(1)
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
            clusterUuid = cluster.uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        VolumeInventory volume = createDataVolume {
            name = "data-volume-3"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::" + host4.uuid]
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction()
        action.sessionId = adminSession()
        action.vmInstanceUuid = vm.uuid
        action.volumeUuid = volume.uuid
        AttachDataVolumeToVmAction.Result ret = action.call()

        // assert ret.error.details.contains("attach volume to VM, no qualified cluster")
        assert ret.error != null

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local.uuid
            clusterUuid = cluster2.uuid
        }
    }

    void testDataVolumeAttachedInMultiPs() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory local = env.inventoryByName("local")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        HostInventory host = env.inventoryByName("kvm")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ClusterInventory cluster = env.inventoryByName("cluster")

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        VmInstanceInventory vm = createVmInstance {
            name = "test-4"
            imageUuid = image.uuid
            cpuNum = 1
            memorySize = gb(1)
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
            clusterUuid = cluster.uuid
        }

        VolumeInventory volume = createDataVolume {
            name = "data-volume-4"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::" + host.uuid]
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction()
        action.sessionId = adminSession()
        action.vmInstanceUuid = vm.uuid
        action.volumeUuid = volume.uuid
        AttachDataVolumeToVmAction.Result ret = action.call()

        assert ret.error == null

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }
    }
}
