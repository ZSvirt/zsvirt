package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ReconnectLocalStorageWhenNoHostCase extends SubCase {
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
        env = env {
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
                    attachPrimaryStorage("local1")
                    attachPrimaryStorage("local2")
                }

                localPrimaryStorage {
                    name = "local1"
                    url = "/local_ps1"
                }
                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testReconnectLocalStorage()
        }
    }

    def testReconnectLocalStorage() {
        HostInventory kvm = env.inventoryByName("kvm")
        PrimaryStorageInventory ps1 = env.inventoryByName("local1")
        PrimaryStorageInventory ps2 = env.inventoryByName("local2")
        ClusterInventory cluster = env.inventoryByName("cluster")

        reconnectPrimaryStorage {
            uuid = ps1.uuid
        }
        reconnectPrimaryStorage {
            uuid = ps2.uuid
        }
        retryInSecs(3, 1) {
            assert ps1.status == PrimaryStorageStatus.Connected.toString()
        }
        retryInSecs(3, 1) {
            assert ps2.status == PrimaryStorageStatus.Connected.toString()
        }

        HostVO hostVo = dbf.findByUuid(kvm.uuid, HostVO.class)
        hostVo.setStatus(HostStatus.Disconnected)
        dbf.updateAndRefresh(hostVo)

        assert hostVo.status == HostStatus.Disconnected

        expect(AssertionError.class) {
            reconnectPrimaryStorage {
                uuid = ps1.uuid
            }
        }
        expect(AssertionError.class) {
            reconnectPrimaryStorage {
                uuid = ps2.uuid
            }
        }

        retryInSecs(3, 1) {
            ps1 = queryPrimaryStorage {
                conditions = ["uuid=${ps1.uuid}".toString()]
            }[0]
            assert ps1.status == PrimaryStorageStatus.Disconnected.toString()
        }
        retryInSecs(3, 1) {
            ps2 = queryPrimaryStorage {
                conditions = ["uuid=${ps2.uuid}".toString()]
            }[0]
            assert ps2.status == PrimaryStorageStatus.Disconnected.toString()
        }

        addKVMHost {
            name = "kvm2"
            managementIp = "127.0.0.2"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }

        reconnectPrimaryStorage {
            uuid = ps1.uuid
        }
        reconnectPrimaryStorage {
            uuid = ps2.uuid
        }

        retryInSecs(3, 1) {
            ps1 = queryPrimaryStorage {
                conditions = ["uuid=${ps1.uuid}".toString()]
            }[0]
            assert ps1.status == PrimaryStorageStatus.Connected.toString()
        }
        retryInSecs(3, 1) {
            ps2 = queryPrimaryStorage {
                conditions = ["uuid=${ps2.uuid}".toString()]
            }[0]
            assert ps2.status == PrimaryStorageStatus.Connected.toString()
        }
    }
}
