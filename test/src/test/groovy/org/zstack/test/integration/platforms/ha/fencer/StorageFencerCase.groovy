package org.zstack.test.integration.platforms.ha.fencer

import org.springframework.http.HttpEntity
import org.zstack.ha.entity.HaCommands
import org.zstack.ha.header.HaConstants
import org.zstack.header.errorcode.SysErrors
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class StorageFencerCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    PrimaryStorageInventory ps
    HostInventory host
    volatile boolean nfsServerConnected = true

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.normal()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testDetachPrimaryStorageFencerCancel()
            installNfsSimulators()
            testAttachPrimaryStorageFencerSetup()
            testUpdatePrimaryStorageUrlFencerUpdate()
            env.cleanMessageHandlers()
        }
    }

    void prepare() {
        host = env.inventoryByName("kvm1") as HostInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory
        ps = env.inventoryByName("nfs") as PrimaryStorageInventory
    }

    void testDetachPrimaryStorageFencerCancel() {
        logger.info("Test 001: expunge the env VM so NFS can leave the cluster")
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }.size() == 0

        boolean called = false
        env.simulator(HaConstants.CANCEL_SELF_FENCER_PATH) {
            called = true
            return new KVMAgentCommands.AgentResponse()
        }

        logger.info("Test 002: detach NFS, expect the self-fencer cancel on the host")
        detachPrimaryStorageFromCluster {
            delegate.primaryStorageUuid = ps.uuid
            delegate.clusterUuid = cluster.uuid
        }
        retryInSecs {
            assert called
        }
        def detached = queryPrimaryStorage {
            delegate.conditions = ["uuid=${ps.uuid}"]
        }[0] as PrimaryStorageInventory
        assert detached.attachedClusterUuids == null || !detached.attachedClusterUuids.contains(cluster.uuid)
    }

    private void installNfsSimulators() {
        env.simulator(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH) { HttpEntity<String> e ->
            def rsp = new NfsPrimaryStorageKVMBackendCommands.MountAgentResponse()
            if (nfsServerConnected) {
                rsp.totalCapacity = gb(100)
                rsp.availableCapacity = gb(100)
            } else {
                rsp.setError("on purpose: mock nfs server down")
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH) { HttpEntity<String> e ->
            def rsp = new NfsPrimaryStorageKVMBackendCommands.UpdateMountPointRsp()
            if (nfsServerConnected) {
                rsp.totalCapacity = gb(100)
                rsp.availableCapacity = gb(100)
            } else {
                rsp.setError("on purpose : mock nfs server down")
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e ->
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (nfsServerConnected) {
                rsp.totalCapacity = gb(100)
                rsp.availableCapacity = gb(100)
            } else {
                rsp.setError("on purpose : mock nfs server down")
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (!nfsServerConnected) {
                rsp.setError("on purpose : mock nfs server down")
            }
            return rsp
        }
    }

    void testAttachPrimaryStorageFencerSetup() {
        boolean fencerCalled = false
        env.simulator(HaConstants.SETUP_SELF_FENCER_PATH) { HttpEntity<String> e ->
            HaCommands.SetupSelfFencerCmd cmd = json(e.body, HaCommands.SetupSelfFencerCmd.class)
            assert cmd.mountedByZStack.get(0)
            assert cmd.mountOptions[0] == ""
            fencerCalled = true
            return new KVMAgentCommands.AgentResponse()
        }

        logger.info("Test 101: attach NFS while the server is down, expect API failure and no fencer setup")
        nfsServerConnected = false
        expectApiFailure({
            attachPrimaryStorageToCluster {
                delegate.primaryStorageUuid = ps.uuid
                delegate.clusterUuid = cluster.uuid
            }
        }) {
            assert delegate.code == SysErrors.OPERATION_ERROR.toString()
        }
        assert !retryInSecs(3, 1) {
            return fencerCalled
        }
        def stillDetached = queryPrimaryStorage {
            delegate.conditions = ["uuid=${ps.uuid}"]
        }[0] as PrimaryStorageInventory
        assert stillDetached.attachedClusterUuids == null || !stillDetached.attachedClusterUuids.contains(cluster.uuid)

        logger.info("Test 102: attach NFS while the server is up, expect fencer setup")
        nfsServerConnected = true
        attachPrimaryStorageToCluster {
            delegate.primaryStorageUuid = ps.uuid
            delegate.clusterUuid = cluster.uuid
        }
        retryInSecs {
            assert fencerCalled
        }
        def attached = queryPrimaryStorage {
            delegate.conditions = ["uuid=${ps.uuid}"]
        }[0] as PrimaryStorageInventory
        assert attached.attachedClusterUuids.contains(cluster.uuid)
    }

    void testUpdatePrimaryStorageUrlFencerUpdate() {
        boolean fencerCalled = false
        env.simulator(HaConstants.SETUP_SELF_FENCER_PATH) {
            fencerCalled = true
            return new KVMAgentCommands.AgentResponse()
        }

        logger.info("Test 201: update the NFS url while the server is down, expect API failure and no fencer setup")
        nfsServerConnected = false
        expectApiFailure({
            updatePrimaryStorage {
                delegate.uuid = ps.uuid
                delegate.url = "localhost:/nfsNew"
            }
        }) {
            assert delegate.code == SysErrors.OPERATION_ERROR.toString()
        }
        assert !retryInSecs(3, 1) {
            return fencerCalled
        }
        assert queryPrimaryStorage {
            delegate.conditions = ["uuid=${ps.uuid}"]
        }[0].url == "localhost:/nfs"

        logger.info("Test 202: reconnect the host, then update the NFS url, expect fencer setup")
        nfsServerConnected = true
        reconnectHost {
            delegate.uuid = host.uuid
        }
        fencerCalled = false
        updatePrimaryStorage {
            delegate.uuid = ps.uuid
            delegate.url = "localhost:/nfsNew"
        }
        retryInSecs {
            assert fencerCalled
        }
        assert queryPrimaryStorage {
            delegate.conditions = ["uuid=${ps.uuid}"]
        }[0].url == "localhost:/nfsNew"
    }

    @Override
    void clean() {
        env.delete()
    }
}
