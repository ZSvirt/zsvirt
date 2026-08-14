package org.zstack.test.integration.image.platform

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.image.ImagePlatform
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/12/26.
 */
class CreateRootVolumeTemplateFromVolumeSnapshotCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VmInstanceInventory vm

    String osType = "centos63"
    String archType = "x86_64"

    VolumeSnapshotInventory rootVolumeSnapshot
    VolumeSnapshotInventory dataVolumeSnapshot

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "username"
                password = "password"
                hostname = "hostname"

                image {
                    name = "image"
                    url = "http://somehost/boot.iso"
                    platform = ImagePlatform.Windows
                    guestOsType = osType
                }

                image {
                    name = "image-no-platform"
                    url = "http://somehost/boot.iso"
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
                name = "vm"
                cpu = 4
                memoryGB(8)
                useImage("image")
                useL3Networks("l3")
                disk {
                    boot = true
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            createSnapshot()

            testCreateImageFromRootVolumeSnapshot()

            // can't not clean VolumeSnapshotTreeVO
            // testCreateImageFromDataVolumeSnapshot()
        }
    }

    void createSnapshot() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        rootVolumeSnapshot = createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "root-volume-snapshot"
        }

        /*
        dataVolumeSnapshot = createVolumeSnapshot {
            volumeUuid = vm.allVolumes.find{ it.uuid != vm.rootVolumeUuid }.uuid
            name = "data-volume-snapshot"
        }
        */
    }

    void testCreateImageFromRootVolumeSnapshot() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        BackupStorageInventory bs = env.inventoryByName("sftp")

        ImageInventory image = createRootVolumeTemplateFromVolumeSnapshot {
            name = "root-volume-snapshot-image"
            snapshotUuid = rootVolumeSnapshot.uuid
            backupStorageUuids = [bs.uuid]
        }
        assert ImagePlatform.Windows.name() == image.platform
        assert osType == image.guestOsType
        assert archType == image.architecture

        def newVm = createVmInstance {
            name = "new-image-vm"
            l3NetworkUuids = [vm.getDefaultL3NetworkUuid()]
            cpuNum = 4
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            imageUuid = vm.imageUuid
        } as VmInstanceInventory
        assert vm.platform == newVm.platform


        String newOsType = "newType"
        image = createRootVolumeTemplateFromVolumeSnapshot {
            name = "root-volume-snapshot-image"
            snapshotUuid = rootVolumeSnapshot.uuid
            platform = ImagePlatform.Windows.name()
            guestOsType = newOsType
            backupStorageUuids = [bs.uuid]
        }
        assert ImagePlatform.Windows.name() == image.platform
        assert newOsType == image.guestOsType
        assert archType == image.architecture
    }

    void testCreateImageFromDataVolumeSnapshot() {
        BackupStorageInventory bs = env.inventoryByName("sftp")

        ImageInventory image = createRootVolumeTemplateFromVolumeSnapshot {
            name = "root-volume-snapshot-image"
            snapshotUuid = dataVolumeSnapshot.uuid
            backupStorageUuids = [bs.uuid]
        }
        assert null == image.guestOsType
        assert ImagePlatform.Linux.name() == image.platform

        image = createRootVolumeTemplateFromVolumeSnapshot {
            name = "root-volume-snapshot-image"
            snapshotUuid = rootVolumeSnapshot.uuid
            platform = ImagePlatform.Windows.name()
            backupStorageUuids = [bs.uuid]
        }
        assert ImagePlatform.Windows.name() == image.platform
    }

    @Override
    void clean() {
        env.delete()
    }

}
