package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/11/29.
 */
class LocalStorageDetachClusterCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    long volumeBitSize = SizeUnit.GIGABYTE.toByte(10)

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            diskOffering {
                name = 'diskOffering'
                diskSize = volumeBitSize
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

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
        }
    }

    @Override
    void test() {
        env.create {
            testDeatchPsAndCheckVmState()
        }
    }

    void testDeatchPsAndCheckVmState() {
        dbf = bean(DatabaseFacade.class)

        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory psInventory = env.inventoryByName("local")
        KVMHostInventory host = env.inventoryByName("kvm")
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        VmInstanceInventory vm = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            cpuNum = 4
            memorySize = gb(8)
            hostUuid = host.uuid
        }

        pauseVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vmVo = dbFindByUuid(vm.uuid,VmInstanceVO.class)
        assert vmVo.state == VmInstanceState.Paused

        detachPrimaryStorageFromCluster{
            primaryStorageUuid = psInventory.uuid
            clusterUuid = cluster.uuid
        }
        vmVo = dbFindByUuid(vm.uuid,VmInstanceVO.class)
        assert vmVo.state == VmInstanceState.Stopped
    }

    @Override
    void clean() {
        env.delete()
    }
}
