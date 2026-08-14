package org.zstack.test.integration.storage.primary

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.*
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.primary.PrimaryStorageManagerImpl
import org.zstack.storage.primary.PrimaryStoragePingTracker
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

/**
 * Created by kayo on 2018/10/31.
 */
class PrimaryStoragePingCase extends SubCase {
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
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps_2"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

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

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

        }
    }

    @Override
    void test() {
        env.create {
            PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1)

            env.simulator(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH) {HttpEntity<String> e, EnvSpec espec ->
                def rsp = new NfsPrimaryStorageKVMBackendCommands.MountAgentResponse()
                rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(10)
                rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(10)
                return rsp
            }

            env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) {HttpEntity<String> e, EnvSpec espec ->
                def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
                rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(10)
                rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(10)
                return rsp
            }

            env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) {HttpEntity<String> e, EnvSpec espec ->
                def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
                rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(10)
                rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(10)
                return rsp
            }

            testNoPingAfterPSDeleted()
            testPingAfterRescan()
            testPingAfterManagementNodeReady()
            testPingSuccessPSReconnectCondition()
        }
    }

    void testPingAfterManagementNodeReady() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        PrimaryStoragePingTracker tracker = bean(PrimaryStoragePingTracker.class)
        PrimaryStorageManagerImpl primaryStorageManager = bean(PrimaryStorageManagerImpl.class)

        // untrack all bs
        tracker.untrackAll()

        int count = 0
        def cleanup = notifyWhenReceivedMessage(PingPrimaryStorageMsg.class) { PingPrimaryStorageMsg msg ->
            if (msg.primaryStorageUuid == ps.uuid) {
                count ++
            }
        }

        sleep(3)

        assert count == 0

        def connectCount = 0
        def cleanup2 = notifyWhenReceivedMessage(ConnectPrimaryStorageMsg.class) { ConnectPrimaryStorageMsg msg ->
            connectCount ++
        }

        // check management node ready
        tracker.managementNodeReady()

        retryInSecs {
            assert count > 0
        }

        primaryStorageManager.managementNodeReady()

        retryInSecs {
            assert connectCount == 2
            assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected).count() == 2
        }

        cleanup()
        cleanup2()
    }

    void testPingSuccessPSReconnectCondition() {
        def zone = env.inventoryByName("zone")
        def cluster = env.inventoryByName("cluster")

        def ps = addNfsPrimaryStorage {
            name = "nfs"
            url = "127.0.0.3:/nfs2"
            zoneUuid = zone.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        boolean pingFail = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.PING_PATH) { rsp, HttpEntity<String> e ->
            NfsPrimaryStorageKVMBackendCommands.PingCmd cmd = JSONObjectUtil.toObject(e.body,  NfsPrimaryStorageKVMBackendCommands.PingCmd)

            if (cmd.uuid == ps.uuid && pingFail) {
                throw new HttpError(503, "on purpose")
            }

            return rsp
        }

        int count = 0
        def cleanup = notifyWhenReceivedMessage(PingPrimaryStorageMsg.class) { PingPrimaryStorageMsg msg ->
            if (msg.primaryStorageUuid == ps.uuid) {
                count ++
            }
        }

        def connectCount = 0
        env.afterSimulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) {rsp, HttpEntity<String> e->
            connectCount++
            sleep(5)
            return rsp
        }

        // make ping fail, bs will disconnected
        pingFail = true

        retryInSecs {
            assert Q.New(PrimaryStorageVO.class)
                    .eq(PrimaryStorageVO_.uuid, ps.uuid)
                    .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Disconnected).isExists()
            assert count > 0
            assert connectCount == 0
        }

        // make ping success will trigger reconnect, and only one connect msg will be sent
        // we do not connect connecting bs
        pingFail = false
        def tmpCount = count

        // same pause time as connect operation
        sleep(5)
        retryInSecs {
            assert Q.New(PrimaryStorageVO.class)
                    .eq(PrimaryStorageVO_.uuid, ps.uuid)
                    .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected).isExists()
            assert count - tmpCount > 1
            assert connectCount == 1
        }

        cleanup()
        env.cleanSimulatorAndMessageHandlers()
    }

    void testPingAfterRescan() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        PrimaryStoragePingTracker tracker = bean(PrimaryStoragePingTracker.class)
        tracker.reScanPrimaryStorage()

        int count = 0

        def cleanup = notifyWhenReceivedMessage(PingPrimaryStorageMsg.class) { PingPrimaryStorageMsg msg ->
            if (msg.primaryStorageUuid == ps.uuid) {
                count ++
            }
        }

        retryInSecs {
            assert count > 0
        }

        cleanup()
    }

    void testNoPingAfterPSDeleted() {
        def zone = env.inventoryByName("zone")
        def cluster = env.inventoryByName("cluster")

        def ps = addNfsPrimaryStorage {
            name = "nfs"
            url = "127.0.0.3:/nfs2"
            zoneUuid = zone.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        int count = 0
        def cleanup = notifyWhenReceivedMessage(PingPrimaryStorageMsg.class) { PingPrimaryStorageMsg msg ->
            if (msg.primaryStorageUuid == ps.uuid) {
                count ++
            }
        }

        retryInSecs {
            assert count > 0
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ps.uuid
        }

        deletePrimaryStorage {
            uuid = ps.uuid
        }

        count = 0

        TimeUnit.SECONDS.sleep(3L)

        assert count == 0

        cleanup()
    }
}
