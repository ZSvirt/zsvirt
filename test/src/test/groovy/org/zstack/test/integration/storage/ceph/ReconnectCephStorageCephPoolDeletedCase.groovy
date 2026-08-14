package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-8.
 */
class ReconnectCephStorageCephPoolDeletedCase extends SubCase{

    EnvSpec env
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone{
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("ceph-pri")
                }

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }

                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

        }
    }

    @Override
    void test() {
        env.create {
            testReconnectCephPrimaryStorage()
            testReconnectCephBackupStorage()
        }
    }

    void testReconnectCephPrimaryStorage(){
        def pri = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        def cmd = null

        env.afterSimulator(CephPrimaryStorageBase.CHECK_POOL_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CheckCmd.class)
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = pri.uuid
        }

        retryInSecs{
            return {
                assert cmd != null
            }
        }
    }
    void testReconnectCephBackupStorage(){
        def back = env.inventoryByName("ceph-bk") as CephBackupStorageInventory
        def cmd = null

        env.afterSimulator(CephBackupStorageBase.CHECK_POOL_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephBackupStorageBase.CheckCmd.class)
            return rsp
        }
        
        reconnectBackupStorage {
            uuid = back.uuid
        }

        retryInSecs{
            return {
                assert cmd != null
            }
        }
    }
    @Override
    void clean() {
        env.delete()
    }
}
