package org.zstack.test.integration.storage.primary.smp

import org.zstack.core.gc.GCStatus
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.storage.primary.smp.SMPPrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/14.
 */
class SMPGCCase extends SubCase {
    EnvSpec env

    PrimaryStorageInventory smp
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
        env = SMPEnv.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            smp = (env.specByName("smp") as PrimaryStorageSpec).inventory

            SMPPrimaryStorageGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            testVolumeGCWhenExpungeVMOnDisconnectedHost()
        }
    }

    void testVolumeGCWhenExpungeVMOnDisconnectedHost() {
        VmInstanceInventory vmInstanceInventory = env.inventoryByName("vm")

        env.afterSimulator(KvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        destroyVmInstance {
            uuid = vmInstanceInventory.uuid
        }

        expungeVmInstance {
            uuid = vmInstanceInventory.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["context~=%${smp.uuid}%".toString()]
        }[0]

        assert inv.status == GCStatus.Idle.toString()

        boolean called = false
        env.afterSimulator(KvmBackend.DELETE_BITS_PATH) { rsp ->
            called = true
            return rsp
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            assert called
            inv = queryGCJob {
                conditions = ["context~=%${smp.uuid}%".toString()]
            }[0]
            assert inv.status == GCStatus.Done.toString()
        }
    }
}
