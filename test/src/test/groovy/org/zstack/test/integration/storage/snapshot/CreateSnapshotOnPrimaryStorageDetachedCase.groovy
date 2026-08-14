package org.zstack.test.integration.storage.snapshot


import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CreateSnapshotOnPrimaryStorageDetachedCase extends SubCase {
    EnvSpec env
    boolean snapshotCalled = false

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void test() {
        env.create {
            env.afterSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { rsp ->
                snapshotCalled = true
                return rsp
            }

            env.afterSimulator(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH) { rsp ->
                snapshotCalled = true
                return rsp
            }

            testLocalStorageCreateSnapshotAfterStorageDetached()
            testNFSCreateSnapshotAfterStorageDetached()
            testSMPCreateSnapshotAfterStorageDetached()
            testCephCreateSnapshotAfterStorageDetached()
        }
    }

    void testCephCreateSnapshotAfterStorageDetached() {
        snapshotCalled = false
        def vm = env.inventoryByName("vm4") as VmInstanceInventory
        def storage = env.inventoryByName("ceph") as PrimaryStorageInventory
        def cluster = env.inventoryByName("cluster4") as ClusterInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = storage.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs {
            assert Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()
        }

        createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "snapshot-1"
        }

        assert snapshotCalled
    }

    void testSMPCreateSnapshotAfterStorageDetached() {
        snapshotCalled = false
        def vm = env.inventoryByName("vm3") as VmInstanceInventory
        def storage = env.inventoryByName("smp") as PrimaryStorageInventory
        def cluster = env.inventoryByName("cluster3") as ClusterInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = storage.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs {
            assert Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()
        }

        expect(AssertionError.class) {
            createVolumeSnapshot {
                volumeUuid = vm.rootVolumeUuid
                name = "snapshot-1"
            }
        }

        assert !snapshotCalled
    }

    void testNFSCreateSnapshotAfterStorageDetached() {
        snapshotCalled = false
        def vm = env.inventoryByName("vm2") as VmInstanceInventory
        def storage = env.inventoryByName("nfs") as PrimaryStorageInventory
        def cluster = env.inventoryByName("cluster2") as ClusterInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = storage.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs {
            assert Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()
        }

        expect(AssertionError.class) {
            createVolumeSnapshot {
                volumeUuid = vm.rootVolumeUuid
                name = "snapshot-1"
            }
        }

        assert !snapshotCalled
    }

    void testLocalStorageCreateSnapshotAfterStorageDetached() {
        snapshotCalled = false
        def vm = env.inventoryByName("vm1") as VmInstanceInventory
        def storage = env.inventoryByName("local") as PrimaryStorageInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = storage.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs {
            assert Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()
        }

        expect(AssertionError.class) {
            createVolumeSnapshot {
                volumeUuid = vm.rootVolumeUuid
                name = "snapshot-1"
            }
        }

        assert !snapshotCalled
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
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            cephBackupStorage {
                name="ceph-bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/image1.qcow2"
                }
            }

            zone {
                name = "zone"

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.0.0.1:/nfs"
                    totalCapacity = SizeUnit.TERABYTE.toByte(10)
                    availableCapacity = SizeUnit.TERABYTE.toByte(8)
                }

                cephPrimaryStorage {
                    name = "ceph"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(80)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.1/?monPort=7777"]
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(90)
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/smp"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(20)
                }

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster3"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster4"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph")
                    attachL2Network("l2")
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


                attachBackupStorage("ceph-bs")
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useCluster("cluster1")
                useHost("kvm1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")
            }

            vm {
                name = "vm2"
                useCluster("cluster2")
                useHost("kvm2")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")
            }

            vm {
                name = "vm3"
                useCluster("cluster3")
                useHost("kvm3")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")
            }

            vm {
                name = "vm4"
                useCluster("cluster4")
                useHost("kvm4")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image1")
            }
        }
    }
}
