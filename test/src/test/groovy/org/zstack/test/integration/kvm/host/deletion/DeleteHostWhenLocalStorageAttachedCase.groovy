package org.zstack.test.integration.kvm.host.deletion

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/7/23.
 */

class DeleteHostWhenLocalStorageAttachedCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
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
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
                useImage("image")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteHostWhenLocalStorageAttached()
        }
    }


    void testDeleteHostWhenLocalStorageAttached() {
        dbf = bean(DatabaseFacade.class)

        VmInstanceInventory vm = env.inventoryByName("vm")
        PrimaryStorageInventory local = env.inventoryByName("local")
        PrimaryStorageInventory local2 = env.inventoryByName("local2")

        VmInstanceInventory vm_local = createVmInstance {
            name = "vm_local"
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            cpuNum = 4
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local.uuid
        }

        VmInstanceInventory vm_local2 = createVmInstance {
            name = "vm_local2"
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            cpuNum = 4
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local2.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local.uuid
            clusterUuid = vm.clusterUuid
        }

        deleteHost {
            uuid = vm.hostUuid
        }

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert null == vmInstanceVO

        vmInstanceVO = dbFindByUuid(vm_local.uuid, VmInstanceVO.class)
        assert null == vmInstanceVO

        vmInstanceVO = dbFindByUuid(vm_local2.uuid, VmInstanceVO.class)
        assert null == vmInstanceVO
    }

    @Override
    void clean() {
        env.delete()
    }
}
