package org.zstack.test.integration.kvm.host.deletion

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceState
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

class DeleteHostWhenLocalStorageDetachedCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                useImage("image")
                useL3Networks("l3")
                useHost("kvm")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useHost("kvm2")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteHostWhenLocalStorageDetached()
        }
    }


    void testDeleteHostWhenLocalStorageDetached() {
        dbf = bean(DatabaseFacade.class)

        VmInstanceInventory vm_kvm = env.inventoryByName("vm")
        VmInstanceInventory vm_kvm2 = env.inventoryByName("vm2")
        PrimaryStorageInventory local = env.inventoryByName("local")

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local.uuid
            clusterUuid = vm_kvm.clusterUuid
        }

        deleteHost {
            uuid = vm_kvm.hostUuid
        }

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm_kvm.uuid, VmInstanceVO.class)
        assert null == vmInstanceVO

        vmInstanceVO = dbFindByUuid(vm_kvm2.uuid, VmInstanceVO.class)
        assert null != vmInstanceVO
        assert vmInstanceVO.state == VmInstanceState.Stopped
    }

    @Override
    void clean() {
        env.delete()
    }
}
