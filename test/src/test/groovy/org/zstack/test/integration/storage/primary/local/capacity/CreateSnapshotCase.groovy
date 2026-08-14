package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageResourceRefVO
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2017/4/21.
 */

/*
 * step 1 : record current ps available capacity
 * step 2 : create snapshot
 * step 3 : check ps available capacity
 * step 4 : reconnect ps,check ps available capacity
 * step 5 : delete snapshot ,check ps available capacity
 * step 6 : reconnect ps,check ps available capacity
 */
class CreateSnapshotCase extends SubCase {
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
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
                        name = "pubL3"

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
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkPSAvailableCapacityAfterCreateSnapshot()
            testDeleteRootSnapshotWhenVmDestroyed()
        }
    }

    void checkPSAvailableCapacityAfterCreateSnapshot(){
        PrimaryStorageInventory ps = env.inventoryByName("local")
        VmInstanceInventory vm = env.inventoryByName("vm")
        LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            volumeUuid = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid }.uuid
            name = "sp1"
        }

        LocalStorageHostRefVO currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        PrimaryStorageInventory currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size


        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]

        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity
    }

    void testDeleteRootSnapshotWhenVmDestroyed() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        def snapshot = createVolumeSnapshot {
            name = "root-volume-snapshot2"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
    }
}
