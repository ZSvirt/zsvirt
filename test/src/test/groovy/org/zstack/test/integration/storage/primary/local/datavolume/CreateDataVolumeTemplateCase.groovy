package org.zstack.test.integration.storage.primary.local.datavolume

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.backup.BackupStorageState
import org.zstack.header.storage.backup.BackupStorageStateEvent
import org.zstack.header.storage.primary.AllocatePrimaryStorageSpaceMsg
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageMsg
import org.zstack.header.storage.primary.GetInstallPathForDataVolumeDownloadMsg
import org.zstack.header.volume.VolumeEO
import org.zstack.header.volume.VolumeVO_
import org.zstack.image.ImageQuotaConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2017/10/19.
 */
class CreateDataVolumeTemplateCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
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
            testCreateDataVolumeTemplate()
            testCreateDataVolumeTemplateQuota()
            testCreateDataVolumeTemplateUponFailure()
        }
    }

    /**
     * 1. create account
     * 2. create vm use the account
     * 3. create data volume use the account
     * 4. update image size quota of the account to 0
     * 5. create template of root or data volume will fail
     * 6. recover image size quota of the account
     * and set image number quota as the used image of the account
     * 7. create template of root or data volume will fail
     */
    void testCreateDataVolumeTemplateQuota() {
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def image = env.inventoryByName("test-iso") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def l3 = env.inventoryByName("pubL3") as L3NetworkInventory

        shareResource {
            resourceUuids = [offer.uuid, disk.uuid, image.uuid, l3.uuid]
            toPublic = true
        }

        def account = createAccount {
            name = "test"
            password = "test"
        } as AccountInventory
        attachPredefineRoles(account.uuid, "vm", "volume", "image")

        def session = logInByAccount {
            accountName = "test"
            password = "test"
        } as SessionInventory

        def vm1 = createVmInstance {
            name = "vm1"
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            rootDiskOfferingUuid = disk.uuid
            sessionId = session.uuid
        } as VmInstanceInventory

        def dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
            sessionId = session.uuid
        } as VolumeInventory

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_SIZE
            value = 0
        }

        expect(AssertionError.class) {
            createDataVolumeTemplateFromVolume {
                name = "data-volume"
                volumeUuid = dataVolume.uuid
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        expect(AssertionError.class) {
            createRootVolumeTemplateFromRootVolume {
                name = "data-volume"
                rootVolumeUuid = vm1.getRootVolumeUuid()
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_SIZE
            value = SizeUnit.GIGABYTE.toByte(1000)
        }

        createDataVolumeTemplateFromVolume {
            name = "root-volume"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
            sessionId = session.uuid
        } as ImageInventory

        def count = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.accountUuid, account.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .count()

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_NUM
            value = count
        }

        expect(AssertionError.class) {
            createDataVolumeTemplateFromVolume {
                name = "data-volume"
                volumeUuid = dataVolume.uuid
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        expect(AssertionError.class) {
            createRootVolumeTemplateFromRootVolume {
                name = "root-volume"
                rootVolumeUuid = vm1.getRootVolumeUuid()
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }
    }

    void testCreateDataVolumeTemplate() {
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory

        def count = Q.New(ImageVO.class).count()

        def dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        def image = createDataVolumeTemplateFromVolume {
            name = "data-volume"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert image.name == "data-volume"

        attachDataVolumeToVm {
            volumeUuid = dataVolume.uuid
            vmInstanceUuid = vm.uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        image = createDataVolumeTemplateFromVolume {
            name = "data-volume-1"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert image.name == "data-volume-1"
        assert Q.New(ImageVO.class).count() == count + 2

        // make sure create data volume is copy from image
        LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd cmd
        env.preSimulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) { HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.getBody(), LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd.class)
        }

        def vol = createDataVolumeFromVolumeTemplate {
            primaryStorageUuid = ps.uuid
            imageUuid = image.uuid
            hostUuid = kvm.uuid
            name = "test"
        } as VolumeInventory

        assert cmd != null
        assert vol.installPath == cmd.primaryStorageInstallPath :
                "you change the create data volume from template logic, make sure the imageCacheVO will be created correctly after volume migrated!!!"
    }

    /**
     * Negative tests for CreateDataVolumeFromVolumeTemplate
     *
     * We inject the following failure conditions, and check that
     * the capacity is not changed.
     *
     * 1. Image is deleted
     * 2. BS is disabled
     * 3. AllocatePrimaryStorageMsg failed
     * 4. GetInstallPathForDataVolumeDownloadMsg failed
     * 5. DownloadDataVolumeToPrimaryStorageMsg failed
     */
    void testCreateDataVolumeTemplateUponFailure() {
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { SftpBackupStorageCommands.DownloadResponse rsp, HttpEntity<String> e ->
            rsp.size = 10240
            rsp.actualSize = 1024
            return rsp
        }

        def image = addImage {
            name = "test-image"
            url = "http://my-site/test.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
            mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate.toString()
        } as ImageInventory

        assert image.size == 10240
        assert image.actualSize == 1024

        // disable BS and check capacity
        changeBackupStorageState {
            uuid = bs.uuid
            stateEvent = BackupStorageStateEvent.disable.toString()
        }

        BackupStorageInventory bss = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert bss.state == BackupStorageState.Disabled.toString()

        GetBackupStorageCapacityResult bsCapacity = getBackupStorageCapacity {
            backupStorageUuids = [bs.uuid]
        } as GetBackupStorageCapacityResult

        GetPrimaryStorageCapacityResult psCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        createDataVolumeFromVolumeTemplate {
            primaryStorageUuid = ps.uuid
            delegate.imageUuid = image.uuid
            delegate.hostUuid = kvm.uuid
            name = "test-success"
        }

        retryInSecs {
            GetBackupStorageCapacityResult currentBsCapacity = getBackupStorageCapacity {
                backupStorageUuids = [bs.uuid]
            } as GetBackupStorageCapacityResult
            GetPrimaryStorageCapacityResult currentPsCapacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            } as GetPrimaryStorageCapacityResult

            assert bsCapacity.availableCapacity == currentBsCapacity.availableCapacity
            assert psCapacity.availableCapacity > currentPsCapacity.availableCapacity
        }

        // pretend allocate PS failed and check capacity
        changeBackupStorageState {
            uuid = bs.uuid
            stateEvent = BackupStorageStateEvent.enable.toString()
        }

        GetPrimaryStorageCapacityResult originPsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        env.message(AllocatePrimaryStorageSpaceMsg.class) { AllocatePrimaryStorageSpaceMsg msg, CloudBus bus ->
            bus.replyErrorByMessageType(msg, "on purpose")
        }

        createVolumeTemplateFailAndCheckCapacity(originPsCapacity, ps.uuid, kvm.uuid, image.uuid)
        env.cleanMessageHandlers()

        // pretend GetInstallPathForDataVolumeDownloadMsg failed
        env.message(GetInstallPathForDataVolumeDownloadMsg.class) {
            GetInstallPathForDataVolumeDownloadMsg msg, CloudBus bus ->
                bus.replyErrorByMessageType(msg, "on purpose")
        }

        createVolumeTemplateFailAndCheckCapacity(originPsCapacity, ps.uuid, kvm.uuid, image.uuid)
        env.cleanMessageHandlers()

        // pretend DownloadDataVolumeToPrimaryStorageMsg failed
        env.message(DownloadDataVolumeToPrimaryStorageMsg.class) {
            DownloadDataVolumeToPrimaryStorageMsg msg, CloudBus bus ->
                bus.replyErrorByMessageType(msg, "on purpose")
        }

        createVolumeTemplateFailAndCheckCapacity(originPsCapacity, ps.uuid, kvm.uuid, image.uuid)
        env.cleanMessageHandlers()

        def originKvmCapacity = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, kvm.uuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue()


        env.hijackSimulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) { rsp, HttpEntity<String> e ->
            rsp.size = image.size
            rsp.actualSize = image.actualSize
            return rsp
        }

        // nothing wrong, we check that capacity has been reserved
        def volume = createDataVolumeFromVolumeTemplate {
            primaryStorageUuid = ps.uuid
            delegate.imageUuid = image.uuid
            hostUuid = kvm.uuid
            name = "test-pass"
        } as VolumeInventory

        assert volume.size == image.size
        assert volume.actualSize == image.actualSize

        GetPrimaryStorageCapacityResult currentPsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        def currentKvmCapacity = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, kvm.uuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue()

        assert originPsCapacity.availableCapacity > currentPsCapacity.availableCapacity
        assert originPsCapacity.availableCapacity - currentPsCapacity.availableCapacity == volume.size

        assert originKvmCapacity > currentKvmCapacity
        assert originKvmCapacity - currentKvmCapacity == volume.size

        // clean-up temporarily generated resources
        deleteDataVolume {
            uuid = volume.uuid
        }

        expungeDataVolume {
            uuid = volume.uuid
        }
        boolean dataVolumeEoExists = Q.New(VolumeEO.class)
                .eq(VolumeVO_.uuid, volume.uuid)
                .isExists()
        assert !dataVolumeEoExists

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }
    }

    void createVolumeTemplateFailAndCheckCapacity(GetPrimaryStorageCapacityResult old, String psUuid, String hostUuid, String imageUuid) {
        expect(AssertionError.class) {
            createDataVolumeFromVolumeTemplate {
                primaryStorageUuid = psUuid
                delegate.imageUuid = imageUuid
                delegate.hostUuid = hostUuid
                name = "test-failure"
            }
        }

        retryInSecs {
            GetPrimaryStorageCapacityResult currentPsCapacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [psUuid]
            } as GetPrimaryStorageCapacityResult

            assert old.availableCapacity == currentPsCapacity.availableCapacity
        }
    }
}
