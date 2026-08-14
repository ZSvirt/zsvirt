package org.zstack.test.integration.storage.snapshot

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/5/18.
 */
class ConcurrentlySnapshotCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm"
                useCluster("cluster")
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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testCreateAndDeleteSnapshotsAfterDestroyVm()
            testDeleteSnapshotsAfterExpungeVm()
            testDeleteSnapshotsAfterDestroyVmDirectly()
        }
    }

    void testCreateAndDeleteSnapshotsAfterDestroyVm() {
        List<String> uuids = new ArrayList<>()
        for (int i = 0; i < 100; i++) {
            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                volumeUuid = vm.getRootVolumeUuid()
                name = "test"
            }

            uuids.add(snapshot.uuid)
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        deleteDataVolume {
            uuid = vm.uuid
        }

        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).count() == (long) 100
        def threads = []
        for (String suuid : uuids) {
            def targetUuid = suuid
            def thread = Thread.start {
                deleteVolumeSnapshot {
                    uuid = targetUuid
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).count() == (long) 0

        recoverVmInstance {
            uuid = vm.uuid
        }
        startVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    void testDeleteSnapshotsAfterExpungeVm() {
        List<String> uuids = new ArrayList<>()
        for (int i = 0; i < 100; i++) {
            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                volumeUuid = vm.getRootVolumeUuid()
                name = "test"
            }

            uuids.add(snapshot.uuid)
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        expungeVmInstance {
            uuid = vm.uuid
        }

        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).count() == (long) 0
        def threads = []
        for (String suuid : uuids) {
            def targetUuid = suuid
            def thread = Thread.start {
                deleteVolumeSnapshot {
                    uuid = targetUuid
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).count() == (long) 0
    }

    void testDeleteSnapshotsAfterDestroyVmDirectly() {
        ClusterInventory cluster = env.inventoryByName("cluster")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm"
            clusterUuid = cluster.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
        }

        createVolumeSnapshot {
            volumeUuid = vm2.getRootVolumeUuid()
            name = "test"
        }

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString())
        destroyVmInstance {
            uuid = vm2.uuid
        }

        List<String> afterDestroyUuids = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid).eq(VolumeSnapshotVO_.volumeUuid, vm2.getRootVolumeUuid()).listValues()
        assert afterDestroyUuids.size() == 0
    }
}
