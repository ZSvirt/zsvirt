package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.Constants
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ReconnectHostAction
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.NfsEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple

/**
 * Created by MaJin on 2017-05-08.
 */
class MultiNfsAttachMultiClusterMultiHostCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vmInv1, vmInv2
    HostInventory host1, host2, host3
    PrimaryStorageInventory ps1, ps2
    ImageInventory image
    L3NetworkInventory l3
    InstanceOfferingInventory ins
    DiskOfferingInventory diskOffering

    Set<List<String>> disConnectHostPsRef = []
    boolean remountCalled = false
    CloudBus bus

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
        env = NfsEnv.TwoNfsTwoClusterThreeHost()
    }

    @Override
    void test() {
        env.create {
            vmInv1 = env.inventoryByName("vm1") as VmInstanceInventory // in host 1 , cluster 1
            vmInv2 = env.inventoryByName("vm2") as VmInstanceInventory // in host 2 , cluster 1
            host1 = env.inventoryByName("kvm1") as HostInventory // in cluster 1
            host2 = env.inventoryByName("kvm2") as HostInventory // in cluster 1
            host3 = env.inventoryByName("kvm3") as HostInventory // in cluster 2
            ps1 = env.inventoryByName("nfs1") as PrimaryStorageInventory // attach cluster 1, cluster 2
            ps2 = env.inventoryByName("nfs2") as PrimaryStorageInventory // attach cluster 1
            image = env.inventoryByName("image1") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            ins = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory

            bus = bean(CloudBus)
            simulatorEnv()
            testCreateAndStartVmNotOnHostDisconnectNfs()
            testReconnectHostNoNfsAccessed()
            testReconnectHostOneNfsNotAccessed()
            testReconnectNfsOneHostNotAccessed()
            testReconnectNfsNoHostAccessed()
            testCreateVmFailedOnHostConnectingNfs()
            env.cleanSimulatorHandlers()
            testDetachNfsFromCluster()
            testDeleteHost()
        }
    }

    void simulatorEnv() {
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (isDisconnect(huuid, cmd.uuid)) {
                rsp.setError("Connection error")
                rsp.setSuccess(false)
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            remountCalled = true
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            if (isDisconnect(huuid, cmd.uuid)) {
                rsp.setError("Connection error")
                rsp.setSuccess(false)
            } else {
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
            }
            return rsp
        }
    }

    void testCreateAndStartVmNotOnHostDisconnectNfs(){
        disconnectHostPS(host1.uuid, ps1.uuid)
        disconnectHostPS(host2.uuid, ps2.uuid)

        expectApiFailure ({
            createVmInstance {
                name = "vm_failed"
                cpuNum = 4
                memorySize = gb(8)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                diskAOs = [
                    [
                        "boot" : true,
                        "primaryStorageUuid" : ps1.uuid,
                    ],
                    [
                        "size" : gb(20),
                        "primaryStorageUuid" : ps2.uuid,
                    ]
                ]
            }
        }) {
            assert delegate.code == "HOST_ALLOCATION.1001"
        }

        recoverConnectHostPS()
        disconnectHostPS(host3.uuid, ps1.uuid)
        disconnectHostPS(host2.uuid, ps2.uuid)
        VmInstanceInventory vm3 = createVmInstance {
            name = "vm3"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : ps1.uuid,
                ],
                [
                    "size" : gb(20),
                    "primaryStorageUuid" : ps2.uuid,
                ]
            ]
        } as VmInstanceInventory

        assert vm3.hostUuid == host1.uuid
        assert vm3.allVolumes.size() == 2
        assert (vm3.allVolumes as List<VolumeInventory>).primaryStorageUuid.containsAll([ps1.uuid, ps2.uuid])

        stopVmInstance {
            uuid = vm3.uuid
        }

        assert retryInSecs(){
            return Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                    .eq(VmInstanceVO_.uuid, vm3.uuid).findValue() == VmInstanceState.Stopped
        }

        recoverConnectHostPS()
        disconnectHostPS(host1.uuid, ps1.uuid)
        disconnectHostPS(host3.uuid, ps1.uuid)
        startVmInstance {
            uuid = vm3.uuid
        }

        retryInSecs() {
            Tuple t = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vm3.uuid).findTuple()
            return {
                assert t.get(0) == VmInstanceState.Running
                assert t.get(1) == host2.uuid
            }
        }

        recoverConnectHostPS()
    }

    void testCreateVmFailedOnHostConnectingNfs(){
        connectingHostPS(host1.uuid, ps1.uuid)
        connectingHostPS(host1.uuid, ps2.uuid)
        connectingHostPS(host2.uuid, ps1.uuid)
        connectingHostPS(host2.uuid, ps2.uuid)
        connectingHostPS(host3.uuid, ps1.uuid)
        connectingHostPS(host3.uuid, ps2.uuid)

        expectApiFailure ({
            createVmInstance {
                name = "vm4"
                cpuNum = 4
                memorySize = gb(8)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                diskAOs = [
                    [
                        "boot" : true,
                        "primaryStorageUuid" : ps1.uuid,
                    ],
                    [
                        "size" : gb(20),
                        "primaryStorageUuid" : ps2.uuid,
                    ]
                ]
            }
        }) {
            assert delegate.code == "HOST_ALLOCATION.1001"
        }
    }

    void testReconnectHostNoNfsAccessed(){
        disconnectHostPS(host1.uuid, ps1.uuid)
        disconnectHostPS(host1.uuid, ps2.uuid)
        ReconnectHostAction a = new ReconnectHostAction();
        a.uuid = host1.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null
        assert Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .count() == 2L

        recoverConnectHostPS()
    }

    void testReconnectHostOneNfsNotAccessed(){
        remountCalled = false
        disconnectHostPS(host1.uuid, ps1.uuid)
        reconnectHost {
            uuid = host1.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert remountCalled
        assert Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.status)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .findValue() == PrimaryStorageHostStatus.Disconnected
        assert Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.status)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps2.uuid)
                .findValue() == PrimaryStorageHostStatus.Connected

        recoverConnectHostPS()
    }

    void testReconnectNfsOneHostNotAccessed() {
        recoverHost()
        disconnectHostPS(host2.uuid, ps1.uuid)
        reconnectPrimaryStorage {
            uuid = ps1.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.status)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .findValue() == PrimaryStorageHostStatus.Connected
        assert Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.status)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host2.uuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .findValue() == PrimaryStorageHostStatus.Disconnected

        recoverConnectHostPS()
    }

    void testReconnectNfsNoHostAccessed(){
        recoverHost()
        remountCalled = false
        disconnectHostPS(host1.uuid, ps1.uuid)
        disconnectHostPS(host2.uuid, ps1.uuid)
        disconnectHostPS(host3.uuid, ps1.uuid)

        ReconnectPrimaryStorageAction a = new ReconnectPrimaryStorageAction()
        a.uuid = ps1.uuid
        a.sessionId = currentEnvSpec.session.uuid

        def ret = a.call()
        assert ret.error != null
        assert remountCalled
        assert Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .count() == 3L
    }

    void testDetachNfsFromCluster(){
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps1.uuid
            clusterUuid = host1.clusterUuid
            sessionId = currentEnvSpec.session.uuid
        }
        assert !Q.New(PrimaryStorageHostRefVO.class)
                .in(PrimaryStorageHostRefVO_.hostUuid, [host1.uuid, host2.uuid])
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid).isExists()

    }

    void testDeleteHost(){
        deleteHost {
            uuid = host3.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert !Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host3.uuid)
                .isExists()

    }

    void recoverConnectHostPS() {
        disConnectHostPsRef.clear()
        SQL.New(PrimaryStorageHostRefVO.class)
                .set(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                .update()
    }

    void disconnectHostPS(String hostUuid, String psUuid) {
        disConnectHostPsRef.add([hostUuid, psUuid])
        SQL.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .set(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .update()
    }

    void connectingHostPS(String hostUuid, String psUuid) {
        SQL.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .set(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connecting)
                .update()
    }

    boolean isDisconnect(String hostUuid, String psUuid) {
        disConnectHostPsRef.contains([hostUuid, psUuid])
    }

    void recoverHost() {
        [host1.uuid, host2.uuid, host3.uuid].forEach{ huuid ->
            reconnectHost {
                uuid = huuid
            }
        }
    }
}
