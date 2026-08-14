package org.zstack.test.integration.storage.snapshot

import org.zstack.core.Platform
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeType
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class SnapshotProtectorCase extends SubCase {
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
        env = makeEnv {
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
                    name = "smp-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "smp-host"
                        managementIp = "127.0.0.5"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                cluster {
                    name = "nfs-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "nfs-host"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-host"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
                }

                cluster {
                    name = "local-cluster"
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

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost1:/nfs"
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
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

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "ceph-image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "local-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useCluster("local-cluster")
            }

            vm {
                name = "nfs-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useCluster("nfs-cluster")
            }

            vm {
                name = "smp-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useCluster("smp-cluster")
            }

            vm {
                name = "ceph-vm"
                useInstanceOffering("instanceOffering")
                useImage("ceph-image")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useCluster("ceph-cluster")
            }
        }
    }

    void testLocalStorageProtector() {
        VmInstanceInventory vm = env.inventoryByName("local-vm")
        VolumeInventory vol = vm.getAllVolumes().find { VolumeInventory v -> v.type == VolumeType.Data.toString() }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "local-vm-sp"
        }

        String origin = sp.primaryStorageInstallPath
        // the VolumeSnpashotProtector will stop deleting a snapshot not belong to its owner volume
        String fake = sp.primaryStorageInstallPath.replaceAll(vol.uuid, Platform.uuid)
        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, fake)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        expectError {
            deleteVolumeSnapshot {
                uuid = sp.uuid
            }
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, origin)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }
    }

    void testNFSProtector() {
        VmInstanceInventory vm = env.inventoryByName("nfs-vm")
        VolumeInventory vol = vm.getAllVolumes().find { VolumeInventory v -> v.type == VolumeType.Data.toString() }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "nfs-vm-sp"
        }

        String origin = sp.primaryStorageInstallPath
        // the VolumeSnpashotProtector will stop deleting a snapshot not belong to its owner volume
        String fake = sp.primaryStorageInstallPath.replaceAll(vol.uuid, Platform.uuid)
        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, fake)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        expectError {
            deleteVolumeSnapshot {
                uuid = sp.uuid
            }
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, origin)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }
    }

    void testCephProtector() {
        VmInstanceInventory vm = env.inventoryByName("ceph-vm")
        VolumeInventory vol = vm.getAllVolumes().find { VolumeInventory v -> v.type == VolumeType.Data.toString() }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "ceph-vm-sp"
        }

        String origin = sp.primaryStorageInstallPath
        // the VolumeSnpashotProtector will stop deleting a snapshot not belong to its owner volume
        String fake = sp.primaryStorageInstallPath.replaceAll(vol.uuid, Platform.uuid)
        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, fake)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        expectError {
            deleteVolumeSnapshot {
                uuid = sp.uuid
            }
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, origin)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }
    }

    void testSMPProtector() {
        VmInstanceInventory vm = env.inventoryByName("smp-vm")
        VolumeInventory vol = vm.getAllVolumes().find { VolumeInventory v -> v.type == VolumeType.Data.toString() }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "smp-vm-sp"
        }

        String origin = sp.primaryStorageInstallPath
        // the VolumeSnpashotProtector will stop deleting a snapshot not belong to its owner volume
        String fake = sp.primaryStorageInstallPath.replaceAll(vol.uuid, Platform.uuid)
        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, fake)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        expectError {
            deleteVolumeSnapshot {
                uuid = sp.uuid
            }
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.primaryStorageInstallPath, origin)
                .eq(VolumeSnapshotVO_.uuid, sp.uuid).update()

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }
    }


    @Override
    void test() {
        env.create {
            testLocalStorageProtector()
            testNFSProtector()
            testCephProtector()
            testSMPProtector()
        }
    }
}
