package org.zstack.test.integration.storage.primary.local_nfs.allocator

import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-10-09.
 */
class MultiPsStartVmCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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

                    attachPrimaryStorage("nfs")
                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
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
            createVmVolumeSizeEqualMultiPsCap()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void createVmVolumeSizeEqualMultiPsCap() {
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory

        def vm = createVmInstance {
            name = "newVm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot": true,
                    "primaryStorageUuid": nfs.uuid
                ],
                [
                    "size": gb(100),
                    "primaryStorageUuid": local.uuid
                ],
                [
                    "size": gb(100),
                    "primaryStorageUuid": local.uuid
                ]
            ]
        } as VmInstanceInventory

        String hostUuid = vm.hostUuid
        for(int i =0; i < 20; i++){
            stopVmInstance {
                delegate.uuid = vm.uuid
            }
            vm = startVmInstance {
                delegate.uuid = vm.uuid
            } as VmInstanceInventory
            assert hostUuid == vm.hostUuid
        }
    }

}
