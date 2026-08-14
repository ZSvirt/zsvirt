package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageDeleteBitsGC
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
/**
 * Created by xing5 on 2017/3/6.
 */
class LocalStorageGCCase extends SubCase {
    EnvSpec env

    DiskOfferingInventory diskOffering
    PrimaryStorageInventory local
    HostInventory host

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
        env = Env.localStorageOneVmEnv()
    }

    void testGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.preSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.preSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            called = true
        }

        // reconnect host to trigger the GC
        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            assert called
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testGCCancelledAfterHostDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = local.uuid
        }

        deletePrimaryStorage {
            uuid = local.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testSkipVolumeGCWhenVolumeInUse() {
        def call
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { LocalStorageKvmBackend.DeleteBitsRsp rsp ->
            call = true
            rsp.setError("volume in use")
            rsp.inUse = true
            return rsp
        }

        VolumeInventory vol = createDataVolume {
            name = "test-volume-in-use"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        } as VolumeInventory

        deleteDataVolume {
            uuid = vol.uuid
        }

        expectError {
            expungeDataVolume {
                uuid = vol.uuid
            }
        }

        assert call
        assert Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vol.uuid).isExists()
        assert queryGCJob {
            conditions = ["context~=%${vol.uuid}%"]
        }[0] == null

        call = false
        LocalStorageDeleteBitsGC gc = new LocalStorageDeleteBitsGC()
        gc.isDir = false
        gc.primaryStorageUuid = local.uuid
        gc.hostUuid = host.uuid
        gc.installPath = vol.getInstallPath()
        gc.NAME = String.format("gc-local-storage-%s-delete-bits-on-host-%s", local.uuid, host.uuid)
        gc.submit()

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
            local = (env.specByName("local") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory
            host = (env.specByName("kvm") as HostSpec).inventory

            testSkipVolumeGCWhenVolumeInUse()
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            testGCSuccess()
            testGCCancelledAfterHostDeleted()

            host = (env.recreate("kvm") as HostSpec).inventory

            testGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
