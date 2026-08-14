package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.AccountResourceRefInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.header.longjob.LongJobState
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 2/6/18.
 */
class CreateRootVolumeTemplateLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    BackupStorageInventory bs
    VmInstanceInventory vm
    String myDescription

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
            testApiMessageValidator()
            testCreateRootVolumeTemplate()
            testCreateRootVolumeTemplateAppointResourceUuid()
            testCreateRootVolumeOnNormalAccountResource()
        }
    }

    void testApiMessageValidator() {
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        vm = env.inventoryByName("vm") as VmInstanceInventory
        gson = new Gson()

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString() + "test"
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)

        expect(AssertionError.class) {
            submitLongJob {
                jobName = msg.getClass().getSimpleName()
                jobData = gson.toJson(msg)
            }
        }
    }

    void testCreateRootVolumeTemplate() {
        int oldSize = Q.New(ImageVO.class).list().size()
        int flag = 0
        myDescription = "my-test"

        env.afterSimulator(LocalStorageKvmBackend.ESTIMATE_TEMPLATE_SIZE_PATH) { Object response ->
            // EstimateTemplateSizeMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, myDescription).find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        env.afterSimulator(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME) { Object response ->
            //CreateTemplateFromVmRootVolumeMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, myDescription).find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        env.afterSimulator(SftpBackupStorageConstant.GET_IMAGE_SIZE) { Object response ->
            //SyncImageSizeMsg -> BackupStorageAskInstallPathMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, myDescription).find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString()
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        int newSize = Q.New(ImageVO.class).list().size()
        assert newSize > oldSize
        assert 3 == flag
    }

    void testCreateRootVolumeTemplateAppointResourceUuid() {
        myDescription = "my-test3"
        String uuid = Platform.uuid

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString()
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)
        msg.resourceUuid = uuid

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        assert null != dbFindByUuid(uuid, ImageVO.class)
    }

    void testCreateRootVolumeOnNormalAccountResource() {
        myDescription = "account-test"

        AccountInventory normalAccount = createAccount {
            name = "test"
            password = "password"
        } as AccountInventory

        changeResourceOwner{
            accountUuid = normalAccount.uuid
            resourceUuid = vm.uuid
        }

        String uuid = Platform.uuid

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString()
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)
        msg.resourceUuid = uuid

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        def ref = (queryAccountResourceRef {
            conditions = ["resourceUuid=${uuid}", "type=Own"]
        } as List<AccountResourceRefInventory>)[0]

        assert currentEnvSpec.session.accountUuid == ref.accountUuid
    }
}
