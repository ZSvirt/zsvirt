package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/4/21.
 */

class SystemUsedCapacityByDeleteHostCase extends SubCase {
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
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

            }
        }
    }

    @Override
    void test() {
        env.create {
            checkPSSystemUsedCapacityAfterDeleteHost()
        }
    }

    void checkPSSystemUsedCapacityAfterDeleteHost(){
        PrimaryStorageInventory ps = env.inventoryByName("local")
        HostInventory host = env.inventoryByName("kvm")

        long originSystemUsedCapacity = ps.systemUsedCapacity

        boolean checked = false
        env.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { rsp, HttpEntity<String> e ->
            rsp.totalCapacity = 2
            rsp.availableCapacity = 1
            checked = true
            return rsp
        }

        HostInventory host1 = addKVMHost {
            name = "host1"
            managementIp = "127.0.0.3"
            username = "root"
            password = "password"
            clusterUuid = host.clusterUuid
        }
        assert checked
        LocalStorageHostRefVO hostRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, host.uuid).find()
        LocalStorageHostRefVO host1RefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, host1.uuid).find()
        ps = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        assert hostRefVO.systemUsedCapacity + host1RefVO.systemUsedCapacity == ps.systemUsedCapacity
        checked = false

        deleteHost {
            uuid = host1.uuid
        }
        assert false == Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, host1.uuid).isExists()
        hostRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, host.uuid).find()
        ps = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        assert !checked
        assert hostRefVO.systemUsedCapacity == ps.systemUsedCapacity
        assert originSystemUsedCapacity == ps.systemUsedCapacity

    }

}
