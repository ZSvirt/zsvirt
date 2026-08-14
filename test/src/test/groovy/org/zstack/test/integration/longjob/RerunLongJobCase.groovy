package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.image.*
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.backup.DownloadImageMsg
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.sdk.RerunLongJobAction
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by GuoYi on 2018/9/12.
 */
class RerunLongJobCase extends SubCase {
    EnvSpec env

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
            testRerunLongJobCase()
        }
    }

    void testRerunLongJobCase() {
        Gson gson = new Gson()
        ErrorFacade errf = bean(ErrorFacade.class)
        BackupStorageInventory bs = env.inventoryByName("sftp") as BackupStorageInventory
        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())

        // failed to add image
        env.message(DownloadImageMsg.class) { DownloadImageMsg dmsg, CloudBus bus ->
            def r = new MessageReply()
            r.setError(errf.instantiateErrorCode("failed to download image", "on purpose"))
            bus.reply(msg, r)
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Failed
        }

        // resume add image job
        env.cleanSimulatorAndMessageHandlers()

        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.uuid).set(LongJobVO_.managementNodeUuid, null).update()
        jobInv = rerunLongJob {
            uuid = jobInv.uuid
        } as LongJobInventory
        assert jobInv.state == org.zstack.sdk.LongJobState.Running
        assert jobInv.managementNodeUuid == Platform.managementServerId

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        assert Q.New(ImageVO.class).eq(ImageVO_.name, "TinyLinux").count() == 1

        deleteBackupStorage {
            uuid = bs.uuid
        }

        RerunLongJobAction action = new RerunLongJobAction()
        action.uuid = jobInv.uuid
        action.sessionId = adminSession()
        RerunLongJobAction.Result ret = action.call()
        ret.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
    }
}
