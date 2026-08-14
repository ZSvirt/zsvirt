package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.zstack.core.agent.AgentConstant
import org.zstack.core.db.Q
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by camile on 2018/3/8.
 */
class CreateDataVolumeTemplateLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    VolumeInventory dataVolume
    BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testCreateDataVolumeTemplateFailure()
            testCreateDataVolumeTemplateSuccess()
            testDeleteAfterCancel()
        }
    }

    void testDeleteAfterCancel() {
        long oldCount = Q.New(ImageVO.class).count()
        APICreateDataVolumeTemplateFromVolumeMsg msg = new APICreateDataVolumeTemplateFromVolumeMsg()
        msg.name = "data-volume-name"
        msg.volumeUuid = dataVolume.uuid
        msg.backupStorageUuids = [bs.uuid]

        boolean canceled = false
        String hostUuid

        env.afterSimulator(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME){ rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AgentCommand.class)
            hostUuid = e.getHeaders().getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            while (!canceled){
                sleep(500)
            }

            rsp.error = "job canceled"
            rsp.success = false
            return rsp
        }

        LongJobInventory jobInv
        env.afterSimulator(AgentConstant.CANCEL_JOB){ rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body,KVMAgentCommands.CancelCmd.class)
            def canceledHostUuid = e.getHeaders().getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            assert cmd.cancellationApiId == jobInv.apiId
            assert canceledHostUuid == hostUuid
            canceled = true
            return rsp
        }

        jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        while (hostUuid == null){
            sleep(500)
        }

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state.toString() == LongJobState.Running.toString()

        cancelLongJob {
            uuid = jobInv.uuid
        }

        retryInSecs {
            LongJobVO job = dbFindByUuid(jobInv.uuid,LongJobVO.class)
            assert job.state.toString() == LongJobState.Canceled.toString()
            assert !Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.resourceUuid).isExists()
        }

        def size = Q.New(ImageVO.class).count()
        assert size == oldCount
    }

    void testCreateDataVolumeTemplateFailure() {
        gson = new Gson()
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory

        long oldCount = Q.New(ImageVO.class).count()

        dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        APICreateDataVolumeTemplateFromVolumeMsg badMsg = new APICreateDataVolumeTemplateFromVolumeMsg()
        badMsg.name = "data-volume"
        badMsg.volumeUuid = dataVolume.uuid + 1
        badMsg.backupStorageUuids = [bs.uuid]

        expect(ApiException.class) {
            submitLongJob {
                submitLongJob {
                    jobName = badMsg.getClass().getSimpleName()
                    jobData = gson.toJson(badMsg)
                }
            }
        }

        assert oldCount == Q.New(ImageVO.class).count()
    }

    void testCreateDataVolumeTemplateSuccess() {
        long oldCount = Q.New(ImageVO.class).count()

        APICreateDataVolumeTemplateFromVolumeMsg msg = new APICreateDataVolumeTemplateFromVolumeMsg()
        msg.name = "data-volume"
        msg.volumeUuid = dataVolume.uuid
        msg.backupStorageUuids = [bs.uuid]

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state.toString() == LongJobState.Running.toString()

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

        assert oldCount < Q.New(ImageVO.class).count()
    }
}