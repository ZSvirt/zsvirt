package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.MonStatus
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by kayo on 2018/1/30.
 */
class CephPrimaryStorageStatusCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps

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
                    monUrls = ["root:password@localhost/?monPort=7777",
                               "root:password@127.0.0.3/?monPort=7777",
                               "root:password@127.0.0.4/?monPort=7777"]
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
            ps = env.inventoryByName("ceph-pri")

            PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1)

            testOneMonDownWontCausePrimaryStorageDown()
            testPingFailButNoFailureSetInResponse()
        }
    }

    void testPingFailButNoFailureSetInResponse() {
        env.afterSimulator(CephPrimaryStorageMonBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageMonBase.PingCmd.class)
            CephPrimaryStorageMonBase.PingRsp pingRsp = new CephPrimaryStorageMonBase.PingRsp()
            if (cmd.monAddr.equals( "127.0.0.4:7777")) {
                pingRsp.failure = null
                pingRsp.success = false
                pingRsp.error = "on purpose"
            }

            return pingRsp
        }

        retryInSecs {
            assert Q.New(CephPrimaryStorageMonVO.class)
                    .eq(CephPrimaryStorageMonVO_.monAddr, "127.0.0.4")
                    .eq(CephPrimaryStorageMonVO_.status, MonStatus.Disconnected).isExists()
        }

        env.cleanAfterSimulatorHandlers()

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            assert Q.New(CephPrimaryStorageMonVO.class)
                    .eq(CephPrimaryStorageMonVO_.monAddr, "127.0.0.4")
                    .eq(CephPrimaryStorageMonVO_.status, MonStatus.Connected).isExists()
        }
    }

    void testOneMonDownWontCausePrimaryStorageDown() {
        SQL.New(CephPrimaryStorageMonVO.class)
                .eq(CephPrimaryStorageMonVO_.monAddr, "127.0.0.4")
                .set(CephPrimaryStorageMonVO_.status, MonStatus.Connecting).update()

        boolean noConnecting = true
        boolean mon1Exist = false
        boolean mon2Exist = false
        env.afterSimulator(CephPrimaryStorageMonBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageMonBase.PingCmd.class)
            if (cmd.monAddr.equals( "127.0.0.4:7777")) {
                noConnecting = false
            } else if (cmd.monAddr.equals("127.0.0.3:7777")) {
                mon2Exist = true
            } else if (cmd.monAddr.equals("localhost:7777")) {
                mon1Exist = true
            }

            return rsp
        }

        retryInSecs {
            assert noConnecting
            assert mon1Exist
            assert mon2Exist
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            assert Q.New(CephPrimaryStorageMonVO.class)
                    .eq(CephPrimaryStorageMonVO_.monAddr, "127.0.0.4")
                    .eq(CephPrimaryStorageMonVO_.status, MonStatus.Connected).isExists()
        }

        env.cleanAfterSimulatorHandlers()
    }
}
