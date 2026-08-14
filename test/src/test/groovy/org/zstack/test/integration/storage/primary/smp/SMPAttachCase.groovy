package org.zstack.test.integration.storage.primary.smp

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.Constants
import org.zstack.header.host.HostVO
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.*
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.storage.primary.smp.SMPDeleteVolumeGC
import org.zstack.storage.primary.smp.SMPPrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
/**
 * Created by AlanJager on 2017/3/10.
 */
class SMPAttachCase extends SubCase{
    EnvSpec env
    PrimaryStorageInventory primaryStorageInventory
    ClusterInventory clusterInventory
    HostInventory host1, host2, host3
    DiskOfferingInventory diskOffering
    private static final CLogger logger = Utils.getLogger(SMPAttachCase.class);
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = SMPEnv.threeHostsNoVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            primaryStorageInventory = env.inventoryByName("smp") as PrimaryStorageInventory
            clusterInventory = env.inventoryByName("cluster") as ClusterInventory
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            host3 = env.inventoryByName("kvm3") as HostInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            testAttachingSMPSuccess()
            testSkipVolumeGCWhenVolumeInUse()
            testAttachingSmpWithoutMountPathOnHost()
            testSameMountPathDifferentStorage()
            testCleanIdFileAfterAttachFailed()
            testAttachSmpOccupiedByOtherSmp()
            testAttachSameMountPointSmp()
            testReconnectHost()
            testReconnectSmpWhenNoHostInCluster()
            testBatchAddHost()
        }
    }

    void testSkipVolumeGCWhenVolumeInUse() {
        def call
        env.afterSimulator(KvmBackend.DELETE_BITS_PATH) { KvmBackend.DeleteRsp rsp ->
            call = true
            rsp.error = "volume in use"
            rsp.success = false
            rsp.inUse = true
            return rsp
        }

        VolumeInventory vol = createDataVolume {
            name = "test-volume-in-use"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = primaryStorageInventory.uuid
        } as VolumeInventory

        def volume = org.zstack.header.volume.VolumeInventory.valueOf(dbFindByUuid(vol.uuid, VolumeVO.class))

        deleteDataVolume {
            uuid = vol.uuid
        }

        expectError {
            expungeDataVolume {
                uuid = vol.uuid
            }
        }

        assert call
        assert Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).isExists()
        assert queryGCJob {
            conditions = ["context~=%${vol.uuid}%"]
        }[0] == null

        call = false
        SMPDeleteVolumeGC gc = new SMPDeleteVolumeGC();
        gc.NAME = String.format("gc-smp-%s-volume-%s", primaryStorageInventory.uuid, vol.uuid)
        gc.primaryStorageUuid = primaryStorageInventory.uuid
        gc.hypervisorType = "KVM"
        gc.volume = volume
        gc.deduplicateSubmit(SMPPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS)

        triggerGCJob {
            uuid = gc.uuid
        }
        retryInSecs {
            assert call
            assert queryGCJob {
                conditions = ["context~=%${vol.uuid}%"]
            }[0].status == GCStatus.Done.toString()
        }
        env.cleanSimulatorAndMessageHandlers()
    }

    void testAttachingSMPSuccess() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        assert Q.New(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterInventory.uuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageInventory.uuid).isExists()
    }

    void testAttachingSmpWithoutMountPathOnHost() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        KvmBackend.ConnectCmd cmd = null
        env.afterSimulator(KvmBackend.CONNECT_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ConnectCmd.class)
            def ret = new KvmBackend.ConnectRsp()
            ret.success = false
            ret.error = "failed"
            return ret
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null

        PrimaryStorageVO vo = dbFindByUuid(primaryStorageInventory.uuid, PrimaryStorageVO.class)
        retryInSecs(3) {
            vo = dbFindByUuid(primaryStorageInventory.uuid, PrimaryStorageVO.class)
            assert vo.getAttachedClusterRefs().isEmpty()
            assert cmd != null
        }

        env.cleanSimulatorAndMessageHandlers()
    }

    void testSameMountPathDifferentStorage(){
        AtomicInteger firstCount = new AtomicInteger(0)
        env.simulator(KvmBackend.CONNECT_PATH) {
            def ret = new KvmBackend.ConnectRsp()
            if(firstCount.getAndIncrement() < 2){
                ret.isFirst = true
            }
            return ret
        }

        AtomicInteger callCount = new AtomicInteger(0)
        env.simulator(KvmBackend.DELETE_BITS_PATH){
            logger.debug(String.format("callCount %d", callCount.incrementAndGet()))
            return new KvmBackend.AgentRsp()
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
        retryInSecs{
            assert callCount.intValue() == 2
        }
    }

    void testCleanIdFileAfterAttachFailed(){
        env.simulator(KvmBackend.CONNECT_PATH) { HttpEntity<String> e ->
            def ret = new KvmBackend.ConnectRsp()
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            if(huuid == host1.uuid){
                ret.isFirst = true
            }
            if(huuid == host2.uuid){
                ret.error = "on purpose"
                ret.success = false
            }
            return ret
        }

        String deleteHostUuid
        AtomicInteger callCount = new AtomicInteger(0)
        env.simulator(KvmBackend.DELETE_BITS_PATH){HttpEntity<String> e ->
            deleteHostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            logger.debug(String.format("callCount %d", callCount.incrementAndGet()))
            return new KvmBackend.AgentRsp()
        }

        def a = new AttachPrimaryStorageToClusterAction()
        a.clusterUuid = clusterInventory.uuid
        a.primaryStorageUuid = primaryStorageInventory.uuid
        a.sessionId = adminSession()
        assert a.call().error != null
        retryInSecs(3){
            assert deleteHostUuid == host1.uuid
            assert callCount.intValue() == 1
        }
    }

    void testAttachSmpOccupiedByOtherSmp(){
        KvmBackend.ConnectCmd cmd = null
        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.success = false
            rsp.error = "occupied by other smp!"
            return rsp
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
    }

    void testAttachSameMountPointSmp(){
        env.cleanSimulatorHandlers()
        attachPrimaryStorageToCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        PrimaryStorageInventory smp2 = addSharedMountPointPrimaryStorage {
            name = "test-smp"
            zoneUuid = primaryStorageInventory.zoneUuid
            url = primaryStorageInventory.url
        } as PrimaryStorageInventory

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = smp2.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
    }

    void testReconnectHost(){
        // 1. cluster has no host or no Connected host
        // 2. attach smp
        // 3. add host will success
        // 4. add host whose mount point different will fail
        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.isFirst = true
            return rsp
        }

        boolean deleteCalled = false
        env.simulator(KvmBackend.DELETE_BITS_PATH) {
            deleteCalled = true
            return new KvmBackend.AgentRsp()
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = host1.uuid
            }
        }

        retryInSecs {
            assert deleteCalled
        }

    }

    void testReconnectSmpWhenNoHostInCluster(){
        deleteHost {
            uuid = host1.uuid
        }

        deleteHost {
            uuid = host2.uuid
        }

        deleteHost {
            uuid = host3.uuid
        }

        ReconnectPrimaryStorageAction a = new ReconnectPrimaryStorageAction()
        a.uuid = primaryStorageInventory.uuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null
    }

    void testBatchAddHost() {
        assert !Q.New(HostVO.class).isExists()

        def first = new AtomicBoolean(true)
        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.isFirst = first.compareAndSet(true, false)
            return rsp
        }

        List<Thread> threads = []
        def count = new AtomicInteger(0)
        for (int i = 1; i <= 3; i++) {
            String ip = "127.0.0." + i.toString()
            threads.add(Thread.start {
                addKVMHost {
                    clusterUuid = host1.clusterUuid
                    managementIp = ip
                    name = "kvm-" + ip
                    username = "root"
                    password = "password"
                }
                count.addAndGet(1)
            })
        }

        threads.forEach({it.join()})
        assert count.intValue() == 3
    }

    @Override
    void clean() {
        env.delete()
    }
}
