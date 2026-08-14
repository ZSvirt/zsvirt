package org.zstack.test.integration.image

import org.apache.commons.lang.ObjectUtils
import org.apache.commons.lang.enums.EnumUtils
import org.mockito.internal.matchers.Null
import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.ImageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/5.
 */
class ImageGCCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

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
        env = Env.oneSftpEnv
    }

    void testImageGCWhenBackupStorageStatusIsNull(){
        ImageInventory image = env.inventoryByName("image")
        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def count = Q.New(ImageVO.class).count()
        deleteImage {
            uuid = image.uuid
        }
        expungeImage {
            imageUuid = image.uuid
        }
        def size = Q.New(ImageVO.class).count()
        assert count == size + 1

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        SftpBackupStorageCommands.DeleteCmd cmd = null
        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, SftpBackupStorageCommands.DeleteCmd.class)
            return rsp
        }

        BackupStorageInventory bs = env.inventoryByName("sftp")
        def bsUuid = bs.uuid
        BackupStorageVO bss = dbFindByUuid(bsUuid, BackupStorageVO.class)
        assert bss.status == BackupStorageStatus.Connected

        deleteBackupStorage {
            uuid = bsUuid
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            assert cmd == null
            assert !dbIsExists(image.uuid, ImageVO.class)
            inv = queryGCJob {
                conditions = ["context~=%${image.uuid}%".toString()]
            }[0]
            assert inv != null
            assert inv.status == GCStatus.Done.toString()
        }

        bss.setStatus(BackupStorageStatus.Connected)
        dbf.updateAndRefresh(bss)
        assert bss.status == BackupStorageStatus.Connected

    }

    void testBackupStorageDeleteBitGCStatus() {
        ImageInventory image = env.inventoryByName("image")
        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def count = Q.New(ImageVO.class).count()
        deleteImage {
            uuid = image.uuid
        }
        expungeImage {
            imageUuid = image.uuid
        }
        def size = Q.New(ImageVO.class).count()
        assert count == size + 1

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        SftpBackupStorageCommands.DeleteCmd cmd = null
        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, SftpBackupStorageCommands.DeleteCmd.class)
            return rsp
        }

        BackupStorageInventory bs = env.inventoryByName("sftp")
        def bsUuid = bs.uuid
        BackupStorageVO bss = dbFindByUuid(bsUuid, BackupStorageVO.class)
        assert bss.status == BackupStorageStatus.Connected
        bss.setStatus(BackupStorageStatus.Disconnected)
        bss = dbf.updateAndRefresh(bss)

        bss = dbFindByUuid(bsUuid, BackupStorageVO.class)
        assert bss.status == BackupStorageStatus.Disconnected

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            assert cmd == null
            assert !dbIsExists(image.uuid, ImageVO.class)
            inv = queryGCJob {
                conditions = ["context~=%${image.uuid}%".toString()]
            }[0]
            assert inv != null
            assert inv.status == GCStatus.Idle.toString()
        }

        bss.setStatus(BackupStorageStatus.Connected)
        dbf.updateAndRefresh(bss)
        assert bss.status == BackupStorageStatus.Connected
    }

    void testImageGCWhenBackupStorageDisconnect() {
        ImageInventory image = env.inventoryByName("image")

        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        SftpBackupStorageCommands.DeleteCmd cmd = null
        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, SftpBackupStorageCommands.DeleteCmd.class)
            return rsp
        }
        triggerGCJob {
            uuid = inv.uuid
        }

        TimeUnit.SECONDS.sleep(2)

        assert cmd != null
        assert !dbIsExists(image.uuid, ImageVO.class)

        inv = queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv.status == GCStatus.Done.toString()
    }

    void testImageGCCancelledAfterBackupStorageDeleted() {
        ImageInventory image = env.inventoryByName("image")

        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        deleteGCJob {
            uuid = inv.uuid
        }

        assert (queryGCJob {
            conditions = ["context~=%${image.uuid}%".toString()]
        } as List).isEmpty()
    }

    @Override
    void test() {
        env.create {
            // make the interval very long, we use api to trigger the job to test
            ImageGlobalConfig.DELETION_GARBAGE_COLLECTION_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))

            dbf = bean(DatabaseFacade.class)
            testBackupStorageDeleteBitGCStatus()
            env.recreate("image")

            testImageGCWhenBackupStorageDisconnect()
            env.recreate("image")

            testImageGCWhenBackupStorageStatusIsNull()
            env.recreate("image")

            testImageGCCancelledAfterBackupStorageDeleted()
        }
    }
}
