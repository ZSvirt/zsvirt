package org.zstack.test.integration.storage.primary.nfs.capacity

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/5/19.
 */
class AttachNfsWhenNoConnectedHostCase extends SubCase {
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
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
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

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                useInstanceOffering("instanceOffering")
                useImage("iso")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testReconnectPrimaryStorageCapacityRecalculation()
        }
    }

    void testReconnectPrimaryStorageCapacityRecalculation() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ClusterInventory clusterInventory = env.inventoryByName("cluster")
        HostInventory hostInventory = env.inventoryByName("kvm")

        GetPrimaryStorageCapacityResult beforeCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = clusterInventory.uuid
        }

        GetPrimaryStorageCapacityResult capacity

        retryInSecs(2){
            capacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }

            assert 0 == capacity.availableCapacity
            assert 0 == capacity.availablePhysicalCapacity
            assert 0 == capacity.totalPhysicalCapacity
            assert 0 == capacity.totalCapacity
        }

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> e ->
            rsp.success = false
            return rsp
        }
        // mock connect fail
        ReconnectHostAction reconnectHostAction = new ReconnectHostAction(
                uuid: hostInventory.uuid,
                sessionId: Test.currentEnvSpec?.session?.uuid
        )
        assert null != reconnectHostAction.call().error

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = clusterInventory.uuid
        }


        retryInSecs(){
            capacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }

            assert 0 >= capacity.availableCapacity
            assert 0 == capacity.availablePhysicalCapacity
            assert 0 == capacity.totalPhysicalCapacity
            assert 0 == capacity.totalCapacity
        }

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> e ->
            rsp.success = true
            return rsp
        }
        reconnectHost {
            uuid = hostInventory.uuid
        }
        capacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert 0 != capacity.availableCapacity
        assert 0 != capacity.availablePhysicalCapacity
        assert 0 != capacity.totalPhysicalCapacity
        assert 0 != capacity.totalCapacity
    }
}
