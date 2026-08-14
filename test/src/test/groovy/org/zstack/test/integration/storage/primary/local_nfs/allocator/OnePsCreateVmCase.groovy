package org.zstack.test.integration.storage.primary.local_nfs.allocator

import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-06-27.
 */
class OnePsCreateVmCase extends SubCase {
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

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(101)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(101)
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
            createVmVolumeSizeEqualSinglePsCap()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void createVmVolumeSizeEqualSinglePsCap() {
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        expectApiFailure({
            createVmInstance {
                delegate.name = "vm"
                delegate.cpuNum = 1
                delegate.memorySize = gb(1)
                delegate.imageUuid = image.uuid
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.diskAOs = [
                    [
                        "boot" : true,
                    ],
                    [
                        "boot" : false,
                        "size" : gb(102)
                    ]
                ]
            }
        }) {
            assert delegate.code == "SYS.1006"
        }

        createVmInstance {
            delegate.name = "vm"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = image.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "boot" : false,
                    "size" : gb(100)
                ]
            ]
        }
    }
}
