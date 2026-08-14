package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.host.*
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
/**
 * Created by AlanJager on 2017/6/27.
 */
class BasicNfsCase extends SubCase {
    EnvSpec env
    HostInventory host1
    HostInventory host2
    HostInventory host3
    PrimaryStorageInventory ps
    PrimaryStorageInventory ps1
    VmInstanceInventory vm
    VirtualRouterVmVO vr

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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "test-iso1"
                    url  = "http://zstack.org/download/test.iso"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }


                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("nfs1")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                nfsPrimaryStorage {
                    name = "nfs1"
                    url = "localhost1:/nfs"
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

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            host1 = env.inventoryByName("kvm1")
            host2 = env.inventoryByName("kvm2")
            host3 = env.inventoryByName("kvm3")
            ps = env.inventoryByName("nfs")
            ps1 = env.inventoryByName("nfs1")
            vm = env.inventoryByName("vm")
            vr = Q.New(VirtualRouterVmVO.class).find()

            testReconnectPrimaryStorageWillCmdToAllHost()
            testUpdatePrimaryStorageMountPoint()
            testUpdateNfsName()
            testReconnectHostWillRemountNfsPsOnTheHost()
            testConfirmSystemUsedCapacityEqualToNull()
            testUpdatePSWithoutHost()
        }
    }

    private recoverEnvironment(){
        env.cleanSimulatorAndMessageHandlers()
        reconnectHost {
            uuid = host1.uuid
        }
        reconnectHost {
            uuid = host2.uuid
        }
        reconnectHost {
            uuid = host3.uuid
        }
    }

    void testReconnectPrimaryStorageWillCmdToAllHost() {
        final CountDownLatch cmdLatch = new CountDownLatch(3)
        env.afterSimulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){ rsp, HttpEntity<String> e ->
            NfsPrimaryStorageKVMBackendCommands.RemountCmd cmd = json(e.body, NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            cmdLatch.countDown()
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        cmdLatch.await(10, TimeUnit.SECONDS)

        env.afterSimulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){ rsp, HttpEntity<String> e ->
            rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.error = "on purpose"
            rsp.success = false
            return rsp
        }


        final CountDownLatch msgLatch = new CountDownLatch(3)
        env.message(ReconnectHostMsg.class) { ReconnectHostMsg msg, CloudBus bus ->
            def reply = new ReconnectHostReply()
            msgLatch.countDown()
            bus.reply(msg, reply)
        }
        ReconnectPrimaryStorageAction action = new ReconnectPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()
        ReconnectPrimaryStorageAction.Result ret = action.call()
        msgLatch.await(10, TimeUnit.SECONDS)

        retryInSecs {
            assert ret.error != null
            Long numOfHosts = Q.New(HostVO.class).eq(HostVO_.status, HostStatus.Connected).count()
            assert numOfHosts == 0L // due to ping task, with 0.2% failure probability
        }

        recoverEnvironment()
    }

    void testUpdatePrimaryStorageMountPoint() {
        final CountDownLatch cmdLatch = new CountDownLatch(3)
        NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd cmd = null
        env.afterSimulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH){ rsp, HttpEntity<String> e ->
            cmd = json(e.body, NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd.class)
            cmdLatch.countDown()
            return rsp
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        stopVmInstance {
            uuid = vr.uuid
        }

        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Stopped).count() == 2L

        String newMountPoint = "test:/tmp"
        updatePrimaryStorage {
            uuid = ps.uuid
            url = newMountPoint
        }

        cmdLatch.await(10, TimeUnit.SECONDS)
        assert cmd.newMountPoint == newMountPoint

        env.cleanSimulatorAndMessageHandlers()
    }

    void testUpdateNfsName(){
        updatePrimaryStorage {
            uuid = ps.uuid
            name = "nfs_test"
        }
    }

    void testReconnectHostWillRemountNfsPsOnTheHost() {
        NfsPrimaryStorageKVMBackendCommands.RemountCmd cmd
        int count = 0
        env.afterSimulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){ rsp, HttpEntity<String> e ->
            cmd = json(e.body, NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            count++
            return rsp
        }

        reconnectHost {
            uuid = host1.uuid
        }

        assert cmd != null
        assert cmd.uuid == ps.getUuid()
    }

    void testConfirmSystemUsedCapacityEqualToNull() {
        PrimaryStorageCapacityVO capacityVO = dbFindByUuid(ps.uuid, PrimaryStorageCapacityVO.class)

        assert capacityVO.systemUsedCapacity == null
    }

    void testUpdatePSWithoutHost() {
        updatePrimaryStorage {
            uuid = ps1.uuid
            url = "localhost2:/nfs"
        }
    }
}
