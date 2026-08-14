package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.DataSecurityPolicy
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by camile on 2017/8/24.
 */
class CephOperationCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
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

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }


                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "test-vm"
                useCluster("test-cluster")
                useHost("ceph-mon")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testAddPrimaryReplaceMon()
            testAddPrimarySameMon()

            testAddBackupReplaceMon()
            testAddBackupSameMon()

            testCephSnapshotTree()

            testConcurrentChangeStateAndReconnect()
            testSdsAdminPassword()
        }
    }

    void testConcurrentChangeStateAndReconnect() {
        def cephMonConnectCalled = false
        env.afterSimulator(CephPrimaryStorageMonBase.ECHO_PATH) { rsp, HttpEntity<String> e ->
            cephMonConnectCalled = true
            return rsp
        }


        def changeStateThread = Thread.start {
            changePrimaryStorageState {
                uuid = ps.uuid
                stateEvent = "maintain"
            }
        }

        CephPrimaryStorageInventory cephPS
        def reconnectPrimaryStorageThread = Thread.start {
            cephPS = reconnectPrimaryStorage {
                uuid = ps.uuid
            }
        }

        changeStateThread.join()
        reconnectPrimaryStorageThread.join()

        assert Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Maintenance)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .eq(PrimaryStorageVO_.uuid, ps.uuid)
                .isExists()

        assert !cephPS.mons.isEmpty()
        assert cephMonConnectCalled
    }

    void prepare() {
        ps = env.inventoryByName("ceph-pri") as CephPrimaryStorageInventory
        bs = env.inventoryByName("ceph-bk") as CephBackupStorageInventory

        assert ps.pools.securityPolicy == [DataSecurityPolicy.Copy.toString()] * 3
        assert ps.pools.diskUtilization == [0.33f] * 3
        assert ps.pools.replicatedSize == [3] * 3

        assert bs.poolSecurityPolicy == DataSecurityPolicy.ErasureCode.toString()
        assert bs.poolDiskUtilization == 0.67f
        assert bs.poolReplicatedSize == 3
    }

    void testAddPrimaryReplaceMon() {
        AddMonToCephPrimaryStorageAction action = new AddMonToCephPrimaryStorageAction()
        action.uuid = ps.uuid
        action.monUrls = ["root:password@localhost/?monPort=7777"]
        action.sessionId = adminSession()
        assert action.call().error != null
        assert action.call().error.code == "SYS.1007"

        // can't checkout concrete content of error message
        // assert action.call().error.details == "Adding the same Mon node is not allowed"
    }

    void testAddPrimarySameMon() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def action = new AddCephPrimaryStorageAction()
        action.sessionId = adminSession()
        action.name = "test"
        action.zoneUuid = zone.uuid
        action.monUrls = ["root:password@127.0.0.2/?monPort=7777", "root:password@127.0.0.2/?monPort=7778"]
        assert action.call().error != null
        assert action.call().error.code == "SYS.1007"

        // assert action.call().error.details == "Cannot add same host[127.0.0.2] in mons"
    }

    void testAddBackupReplaceMon() {
        AddMonToCephBackupStorageAction action = new AddMonToCephBackupStorageAction()
        action.uuid = bs.uuid
        action.monUrls = ["root:password@localhost/?monPort=7777"]
        action.sessionId = adminSession()
        assert action.call().error != null
        assert action.call().error.code == "SYS.1007"

        // assert action.call().error.details == "Adding the same Mon node is not allowed"
    }

    void testAddBackupSameMon() {
        def action = new AddCephBackupStorageAction()
        action.sessionId = adminSession()
        action.name = "test"
        action.monUrls = ["root:password@127.0.0.2/?monPort=7777", "root:password@127.0.0.2/?monPort=7778"]
        assert action.call().error != null
        assert action.call().error.code == "SYS.1007"
        
        // assert action.call().error.details == "Cannot add same host[127.0.0.2] in mons"
    }

    // for verify JIRA #7853
    void testCephSnapshotTree() {
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory

        createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "s1"
        }
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.current, true).count() == 1

        createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "s1"
        }
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 2
        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.current, true).count() == 1
    }

    void testSdsAdminPassword() {
        assert Q.New(GlobalConfigVO.class)
                .select(GlobalConfigVO_.value)
                .eq(GlobalConfigVO_.name, "sds.admin.password")
                .eq(GlobalConfigVO_.category, "ceph")
                .findValue() == "password"

        updateGlobalConfig {
            category = CephGlobalConfig.CATEGORY
            name = CephGlobalConfig.SDS_ADMIN_PASSWORD.name
            value = "PWD"
            sessionId = adminSession()
        }

        assert Q.New(GlobalConfigVO.class)
                .select(GlobalConfigVO_.value)
                .eq(GlobalConfigVO_.name, "sds.admin.password")
                .eq(GlobalConfigVO_.category, "ceph")
                .findValue() == "PWD"
    }

    @Override
    void clean() {
        env.delete()
    }
}
