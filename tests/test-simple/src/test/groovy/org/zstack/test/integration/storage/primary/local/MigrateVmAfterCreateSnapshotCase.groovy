package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017-12-21.
 */
class MigrateVmAfterCreateSnapshotCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

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

                    kvm {
                        name = "kvm1"
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
                useHost("kvm")
                disk {
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.updateValue(Boolean.TRUE.toString())

            testLiveMigrateWithoutSnapshot()
            testLiveMigrateAfterCreateSnapshot()
            testLiveMigrateAfterReset()
        }
    }

    void testLiveMigrateWithoutSnapshot() {
        VmInstanceInventory vm = (VmInstanceInventory) env.inventoryByName("vm")
        HostInventory srcHost = env.inventoryByName("kvm")
        HostInventory dstHost = env.inventoryByName("kvm1")

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = dstHost.uuid
        }

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = srcHost.uuid
        }
    }

    void testLiveMigrateAfterCreateSnapshot() {
        VmInstanceInventory vm = (VmInstanceInventory) env.inventoryByName("vm")
        HostInventory srcHost = env.inventoryByName("kvm")
        HostInventory dstHost = env.inventoryByName("kvm1")

        createVolumeSnapshot {
            name = "root-volume-snapshot"
            volumeUuid = vm.rootVolumeUuid
        }

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = dstHost.uuid
        }

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = srcHost.uuid
        }
    }

    void testLiveMigrateAfterReset() {
        VmInstanceInventory vm = (VmInstanceInventory) env.inventoryByName("vm")
        HostInventory srcHost = env.inventoryByName("kvm")
        HostInventory dstHost = env.inventoryByName("kvm1")

        stopVmInstance {
            uuid = vm.uuid
        }

        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }


        startVmInstance {
            uuid = vm.uuid
        }

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = dstHost.uuid
        }

        migrateVm {
            vmInstanceUuid = vm.getUuid()
            hostUuid = srcHost.uuid
        }
    }
}
