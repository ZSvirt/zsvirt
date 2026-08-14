package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.longjob.LongJobManager
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by kayo on 2018/4/23.
 */
class LoadLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    VolumeInventory dataVolume
    BackupStorageInventory bs
    LongJobInventory jobInv
    LongJobManager longJobManager

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            gson = new Gson()
            longJobManager = bean(LongJobManager.class)
            testSubmitLongJobCase()
            testLoadCancelingJob()
            testLoadWaitingJob()
        }
    }

    void testSubmitLongJobCase() {
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory

        dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        // check jobName
        APICreateDataVolumeTemplateFromVolumeMsg msg = new APICreateDataVolumeTemplateFromVolumeMsg()
        msg.name = "data-volume"
        msg.volumeUuid = dataVolume.uuid
        msg.backupStorageUuids = [bs.uuid]

        jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

        SQL.New(LongJobVO.class)
                .set(LongJobVO_.state, LongJobState.Running)
                .update()

        SQL.New(LongJobVO.class)
                .set(LongJobVO_.managementNodeUuid, null)
                .update()

        longJobManager.loadLongJob()

        List<LongJobVO> vos = Q.New(LongJobVO.class).notNull(LongJobVO_.managementNodeUuid).list()

        assert vos.size() == 1

        for (LongJobVO vo : vos) {
            assert vo.getState() == LongJobState.Failed
        }
    }

    void testLoadCancelingJob() {
        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.uuid)
                .set(LongJobVO_.managementNodeUuid, null)
                .set(LongJobVO_.state, LongJobState.Canceling)
                .update()

        longJobManager.loadLongJob()

        List<LongJobVO> vos = Q.New(LongJobVO.class).notNull(LongJobVO_.managementNodeUuid).list()

        assert vos.size() == 1

        for (LongJobVO vo : vos) {
            assert vo.getState() == LongJobState.Canceled
        }
    }

    void testLoadWaitingJob() {
        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.uuid)
                .set(LongJobVO_.managementNodeUuid, null)
                .set(LongJobVO_.state, LongJobState.Waiting)
                .update()

        longJobManager.loadLongJob()

        List<LongJobVO> vos = Q.New(LongJobVO.class).notNull(LongJobVO_.managementNodeUuid).list()

        assert vos.size() == 1

        for (LongJobVO vo : vos) {
            assert vo.getState() == LongJobState.Running
        }
    }
}
