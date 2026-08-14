package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.SQL
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.primary.CephDeleteVolumeChainGC
import org.zstack.storage.ceph.primary.CephDeleteVolumeGC
import org.zstack.storage.ceph.primary.CephDeleteVolumeSnapshotGC
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.vfs.VFS
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

/**
 * Created by kayo on 2018/7/25.
 */
class CephGCCase extends SubCase {
    EnvSpec env

    PrimaryStorageInventory ceph
    DiskOfferingInventory diskOffering
    boolean deleteFail = false
    boolean called = false

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
        env = CephEnv.CephStorageOneVmEnv()
    }

    void testVolumeGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        deleteFail = true

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
            assert !called
        }

        assert queryGCJob {
            conditions = ["context~=%${vol.getUuid()}%".toString()]
        }.size == 1

        called = false
        deleteFail = false

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testVolumeGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        deleteFail = true

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory volumeGC = null
        retryInSecs {
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${CephDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("test-cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = ceph.uuid
        }

        deletePrimaryStorage {
            uuid = ceph.uuid
        }

        triggerGCJob {
            uuid = volumeGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${CephDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Done.toString()
        }
    }

    void testVolumeSnapshotGC() {
        env.cleanSimulatorAndMessageHandlers()
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        def sp = createVolumeSnapshot {
            name = "test-gc"
            volumeUuid = vol.uuid
        } as VolumeSnapshotInventory

        def deleteFailed = true
        def deleteSucVol = []
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            if (deleteFailed) {
                rsp.setError("it's children in trash, cannot delete")
            }
            return rsp
        }
        env.simulator(CephPrimaryStorageBase.DELETE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.DeleteRsp()
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteCmd.class)
            if (deleteSucVol.contains(cmd.installPath)) {
                return rsp
            }
            if (deleteFailed) {
                rsp.setError("snapshot exists")
            }
            return rsp
        }
        def callGc = false
        def undeletedInstallPaths = []
        env.hijackSimulator(CephPrimaryStorageBase.DELETE_VOLUME_CHAIN_PATH) { CephPrimaryStorageBase.DeleteVolumeChainRsp rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteVolumeChainCmd.class)
            // mock install path not clean
            rsp.undeletedInstallPaths = cmd.installPaths

            if (callGc) {
                rsp.undeletedInstallPaths = undeletedInstallPaths
            }
            return rsp
        }
        def backingChain = []
        env.hijackSimulator(CephPrimaryStorageBase.GET_BACKING_CHAIN_PATH) { CephPrimaryStorageBase.GetBackingChainRsp rsp, HttpEntity<String> e ->
            rsp.backingChain = backingChain
            return rsp
        }

        deleteDataVolume {
            uuid = vol.uuid
        }
        expungeDataVolume {
            uuid = vol.uuid
        }

        def spGC = queryGCJob {
            conditions = ["runnerClass=${CephDeleteVolumeSnapshotGC.class.name}".toString(), "context~=%${sp.primaryStorageInstallPath}%".toString()]
        } as List<GarbageCollectorInventory>
        assert spGC.size() == 1
        assert spGC[0].status != GCStatus.Done.toString()

        def volGC = queryGCJob {
            conditions = ["runnerClass=${CephDeleteVolumeGC.class.name}".toString(), "context~=%${vol.installPath}%".toString()]
        } as List<GarbageCollectorInventory>
        assert volGC.size() == 1
        assert volGC[0].status != GCStatus.Done.toString()

        deleteFailed = false
        triggerGCJob {
            uuid = spGC[0].uuid
        }

        triggerGCJob {
            uuid = volGC[0].uuid
        }

        retryInSecs {
            assert queryGCJob {
                conditions = ["runnerClass=${CephDeleteVolumeSnapshotGC.class.name}".toString(), "context~=%${sp.primaryStorageInstallPath}%".toString()]
            }[0].status == GCStatus.Done.toString()
            assert queryGCJob {
                conditions = ["runnerClass=${CephDeleteVolumeGC.class.name}".toString(), "context~=%${vol.installPath}%".toString()]
            }[0].status == GCStatus.Done.toString()
        }
        SQL.New(GarbageCollectorVO.class).in(GarbageCollectorVO_.uuid, [volGC[0].uuid, spGC[0].uuid]).hardDelete()
        vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        sp = createVolumeSnapshot {
            name = "test-gc"
            volumeUuid = vol.uuid
        } as VolumeSnapshotInventory

        VolumeInventory incVol = createDataVolumeFromVolumeSnapshot {
            volumeSnapshotUuid = sp.uuid
            name = "incVol"
            systemTags = [VolumeSystemTags.FAST_CREATE.tagFormat]
        }

        def sp2 = createVolumeSnapshot {
            name = "test-gc2"
            volumeUuid = incVol.uuid
        } as VolumeSnapshotInventory

        VolumeInventory incVol2 = createDataVolumeFromVolumeSnapshot {
            volumeSnapshotUuid = sp2.uuid
            name = "incVol2"
            systemTags = [VolumeSystemTags.FAST_CREATE.tagFormat]
        }

        deleteFailed = true
        deleteSucVol = [incVol2.installPath]
        backingChain = [sp.primaryStorageInstallPath]
        deleteDataVolume {
            uuid = vol.uuid
        }
        expungeDataVolume {
            uuid = vol.uuid
        }
        deleteDataVolume {
            uuid = incVol.uuid
        }
        expungeDataVolume {
            uuid = incVol.uuid
        }
        deleteDataVolume {
            uuid = incVol2.uuid
        }
        expungeDataVolume {
            uuid = incVol2.uuid
        }

        retryInSecs {
            spGC = queryGCJob {
                conditions = ["runnerClass=${CephDeleteVolumeChainGC.class.name}".toString()]
            } as List<GarbageCollectorInventory>
            assert spGC.size() == 1
            assert spGC[0].status != GCStatus.Done.toString()
            assert spGC[0].context.contains(sp2.primaryStorageInstallPath)
            assert spGC[0].context.contains(sp.primaryStorageInstallPath)
        }

        deleteFailed = false
        callGc = true
        undeletedInstallPaths = [sp2.primaryStorageInstallPath, sp.primaryStorageInstallPath]
        triggerGCJob {
            uuid = spGC[0].uuid
        }
        triggerGCJob {
            uuid = spGC[0].uuid
        }

        assert !retryInSecs(2) {
            def jobs = queryGCJob {
                conditions = ["runnerClass=${CephDeleteVolumeChainGC.class.name}".toString()]
            } as List<GarbageCollectorInventory>
            // no duplicate gc
            return jobs.size() != 1 || jobs[0].uuid != spGC[0].uuid || jobs[0].context != spGC[0].context
        }

        env.cleanSimulatorAndMessageHandlers()

        SQL.New(GarbageCollectorVO.class).in(GarbageCollectorVO_.uuid, [spGC[0].uuid]).hardDelete()
        vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        sp = createVolumeSnapshot {
            name = "test-gc"
            volumeUuid = vol.uuid
        } as VolumeSnapshotInventory

        incVol = createDataVolumeFromVolumeSnapshot {
            volumeSnapshotUuid = sp.uuid
            name = "incVol"
            systemTags = [VolumeSystemTags.FAST_CREATE.tagFormat]
        }

        deleteDataVolume {
            uuid = vol.uuid
        }
        expungeDataVolume {
            uuid = vol.uuid
        }
        deleteDataVolume {
            uuid = incVol.uuid
        }
        expungeDataVolume {
            uuid = incVol.uuid
        }

        assert !retryInSecs(2) {
            spGC = queryGCJob {
                conditions = ["runnerClass=${CephDeleteVolumeChainGC.class.name}".toString(), "context~=%${sp.primaryStorageInstallPath}%".toString()]
            } as List<GarbageCollectorInventory>
            return spGC.size() > 0
        }
    }

    void prepareEnv() {
        env.preSimulator(CephPrimaryStorageBase.DELETE_PATH) {
            if (deleteFail) {
                throw new HttpError(403, "on purpose")
            }

            called = true
        }
    }

    @Override
    void test() {
        env.create {
            ceph = (env.specByName("ceph-pri") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory

            // set a very long time so the GC won't run, we use API to trigger it
            CephGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            testVolumeSnapshotGC()
            prepareEnv()
            testVolumeGCSuccess()
            testVolumeGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
