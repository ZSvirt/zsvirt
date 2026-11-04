package org.zstack.test.integration.storage.primary.local_nfs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by MaJin on 2017/12/1.
 */
class ReconnectedNfsWhenHostConnectedCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageNfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testReconnectNfsWhenConnectedHostNeverMountItself()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testReconnectNfsWhenConnectedHostNeverMountItself(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        HostInventory host = env.inventoryByName("kvm") as HostInventory

        boolean nfsServerConnected = false, mounted = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH){
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (!mounted){
                rsp.setError("on purpose: mock mount point not existing")
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){ HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (!nfsServerConnected) {
                rsp.setError("on purpose: mock nfs server down")
            } else {
                mounted = true
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
            }
            return rsp
        }

        def a = new ReconnectPrimaryStorageAction()
        a.uuid = nfs.uuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null
        
        assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.status).findValue() == HostStatus.Connected
        assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, nfs.uuid).select(PrimaryStorageVO_.status)
                .findValue() == PrimaryStorageStatus.Disconnected
        Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.primaryStorageUuid, nfs.uuid)
                .select(PrimaryStorageHostRefVO_.status)
                .listValues().forEach({it -> assert it.toString() == PrimaryStorageHostStatus.Disconnected.toString()})

        nfsServerConnected = true
        reconnectPrimaryStorage {
            uuid = nfs.uuid
        }

        assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.status).findValue() == HostStatus.Connected
        assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, nfs.uuid).select(PrimaryStorageVO_.status)
                .findValue() == PrimaryStorageStatus.Connected
        Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.primaryStorageUuid, nfs.uuid)
                .select(PrimaryStorageHostRefVO_.status)
                .listValues().forEach({it -> assert it.toString() == PrimaryStorageHostStatus.Connected.toString()})
        assert mounted
    }
}
