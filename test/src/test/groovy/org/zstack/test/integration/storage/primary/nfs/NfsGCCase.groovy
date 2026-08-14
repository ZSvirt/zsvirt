package org.zstack.test.integration.storage.primary.nfs

import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.*
import org.zstack.storage.primary.nfs.*
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*

import java.util.concurrent.TimeUnit
/**
 * Created by xing5 on 2017/3/5.
 */
class NfsGCCase extends SubCase {
    EnvSpec env

    PrimaryStorageInventory nfs
    DiskOfferingInventory diskOffering

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
        env = Env.nfsOneVmEnv()
    }

    void testVolumeGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        env.preSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.preSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            called = true
        }

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


    void testVolumeSnapshotGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "sp"
        }

        env.preSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }

        GarbageCollectorInventory inv = null
        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.preSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            called = true
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testVolumeSnapshotAndVolumeGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "sp"
        }

        env.preSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory spGC = null
        GarbageCollectorInventory volumeGC = null
        retryInSecs {
            spGC = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeSnapshotGC.class.name}".toString()]
            }[0]

            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert spGC.status == GCStatus.Idle.toString()
            assert volumeGC.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = nfs.uuid
        }

        deletePrimaryStorage {
            uuid = nfs.uuid
        }

        triggerGCJob {
            uuid = spGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            spGC = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeSnapshotGC.class.name}".toString()]
            }[0]

            assert spGC.status == GCStatus.Done.toString()
        }

        triggerGCJob {
            uuid = volumeGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Done.toString()
        }
    }

    void testSkipVolumeGCWhenVolumeInUse() {
        def call
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { NfsPrimaryStorageKVMBackendCommands.DeleteResponse rsp ->
            call = true
            rsp.setError("volume in use")
            rsp.inUse = true
            return rsp
        }

        VolumeInventory vol = createDataVolume {
            name = "test-volume-in-use"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
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
        NfsDeleteVolumeGC gc = new NfsDeleteVolumeGC();
        gc.NAME = String.format("gc-nfs-%s-volume-%s", nfs.uuid, vol.getUuid());
        gc.primaryStorageUuid = nfs.uuid
        gc.hypervisorType ="KVM"
        gc.volume = volume
        gc.submit(NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

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

    @Override
    void test() {
        env.create {
            nfs = (env.specByName("nfs") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory

            NfsPrimaryStorageGlobalConfig.GC_INTERVAL.updateValue(1)
            testSkipVolumeGCWhenVolumeInUse()
            // set a very long time so the GC won't run, we use API to trigger it
            NfsPrimaryStorageGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            testVolumeGCSuccess()
            testVolumeSnapshotGCSuccess()
            testVolumeSnapshotAndVolumeGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
