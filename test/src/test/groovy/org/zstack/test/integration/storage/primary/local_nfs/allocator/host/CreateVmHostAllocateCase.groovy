package org.zstack.test.integration.storage.primary.local_nfs.allocator.host

import org.zstack.compute.allocator.HostPrimaryStorageAllocatorFlow
import org.zstack.core.Platform
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-11-26.
 */
class CreateVmHostAllocateCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(100)
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
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(60)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(60)
                }

                localPrimaryStorage { // no host attach to this ps
                    name = "local2"
                    url = "/local_ps2"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(60)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(60)
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
            testGetCandidateZonesClustersHostsForCreatingVm()
            testAllocateVmWithWrongPs()
            testCreateVmAssignNfs()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testGetCandidateZonesClustersHostsForCreatingVm(){
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        List<HostInventory> hosts = getCandidateZonesClustersHostsForCreatingVm {
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }.getHosts()

        assert 2 == hosts.size()
    }

    void testAllocateVmWithWrongPs() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def image = env.inventoryByName("image") as ImageInventory
        def wrongPs = env.inventoryByName("local2") as PrimaryStorageInventory

        def kvm = env.inventoryByName("kvm") as HostInventory
        def kvm1 = env.inventoryByName("kvm1") as HostInventory

        expectApiFailure({
            createVmInstance {
                delegate.name = "vm2"
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.clusterUuid = cluster.uuid
                delegate.imageUuid = image.uuid
                delegate.primaryStorageUuidForRootVolume = wrongPs.uuid
                delegate.cpuNum = 1
                delegate.memorySize = SizeUnit.GIGABYTE.toByte(1)
                delegate.diskAOs = [
                    [
                        boot : true,
                        platform : "Linux",
                        guestOsType : "Linux",
                        architecture : "x86_64",
                        size : SizeUnit.GIGABYTE.toByte(10)
                    ]
                ]
            }
        }) {
            assert delegate.code == "HOST_ALLOCATION.1001"
            assert delegate.opaque
            assert delegate.opaque["rejectedCandidates"]
            def list = (delegate.opaque["rejectedCandidates"] as List<Map<String, Object>>)
            assert list.size() == 2
            assert list.any { it["hostUuid"] == kvm.uuid && it["hostName"] == "kvm" }
            assert list.any { it["hostUuid"] == kvm1.uuid && it["hostName"] == "kvm1" }
            assert list.every { it["reject"] == "not accessible to the specific primary storage" }
            assert list.every { it["rejectI18n"] == Platform.i18n("not accessible to the specific primary storage") }
            assert list.every { it["rejectBy"] == HostPrimaryStorageAllocatorFlow.class.simpleName }
        }
    }

    void testCreateVmAssignNfs(){
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        HostInventory host = env.inventoryByName("kvm")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        PrimaryStorageInventory local = env.inventoryByName("local")

        createVmInstance {
            name = "newVm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ]
            ]
        }

        createVmInstance {
            name = "newVm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(100),
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(100),
                    "primaryStorageUuid" : nfs.uuid,
                ],
            ]
        }

        expectApiFailure({
            createVmInstance {
                name = "newVm2"
                cpuNum = 1
                memorySize = gb(1)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                hostUuid = host.uuid
                diskAOs = [
                    [
                        "boot" : true,
                        "primaryStorageUuid" : local.uuid,
                    ],
                    [
                        "size" : gb(100),
                        "primaryStorageUuid" : local.uuid,
                    ],
                    [
                        "size" : gb(100),
                        "primaryStorageUuid" : local.uuid,
                    ],
                ]
            }
        }) {
            assert delegate.code == "SYS.1006"
        }
    }
}
