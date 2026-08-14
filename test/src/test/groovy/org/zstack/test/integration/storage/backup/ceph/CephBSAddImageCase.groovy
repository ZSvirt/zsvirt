package org.zstack.test.integration.storage.backup.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.*
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.BackupStorageVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2017/3/31.
 */
class CephBSAddImageCase extends SubCase{
    EnvSpec env
    int failCount = 0
    final bsMonsCount = 2
    SessionInventory normalSession

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
            }

            zone{
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("ceph-pri")
                }

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }

                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777", "root:password@127.0.0.2/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }
            cephBackupStorage {
                name="ceph-bk2"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk2"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a65"
                monUrls = ["root:password@127.0.0.3/?monPort=7777", "root:password@127.0.0.4/?monPort=7777"]
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            simulatorEnv()
            testImageBackupStorageRefVOWhenAddImage()
            testUploadImage()
            testCreateTemplateFromVolume()
            testAddImageButBSHasNoAvailableCapacity()
        }
    }

    void prepare() {
        def account1 = createAccount {
            name = "test"
            password = "password"
        } as AccountInventory
        attachPredefineRoles(account1.uuid, "image")

        normalSession = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory
    }

    void simulatorEnv() {
        env.preSimulator(CephBackupStorageBase.GET_LOCAL_FILE_SIZE) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.DownloadCmd.class)
            if (cmd.url.startsWith("file:/")) {
                if (++failCount < bsMonsCount) {
                    throw new Exception("on purpose")
                } else {
                    failCount = 0
                }
            }
        }

        env.preSimulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.DownloadCmd.class)
            if (cmd.url.startsWith("file:/")) {
                if (++failCount < bsMonsCount) {
                    throw new Exception("on purpose")
                } else {
                    failCount = 0
                }
            }
        }
    }

    void testAddImageButBSHasNoAvailableCapacity() {
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")

        SQL.New(BackupStorageVO.class)
                .set(BackupStorageVO_.availableCapacity, Long.valueOf(0))
                .eq(BackupStorageVO_.uuid, bs.uuid).update()

        expect(AssertionError.class) {
            addImage {
                name = "large-image"
                url = "http://my-site/ftest.iso"
                backupStorageUuids = [bs.uuid]
                format = ImageConstant.ISO_FORMAT_STRING
            }
        }
    }

    void testImageBackupStorageRefVOWhenAddImage(){
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")
        BackupStorageInventory bs2 = env.inventoryByName("ceph-bk2")

        ImageInventory newImage = addImage {
            name = "large-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage1 = addImage {
            name = "image2"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage2 = addImage {
            name = "image3"
            url = "file:///my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage3 = addImage {
            name = "image4"
            url = "/my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        //if the url starting with "/" or "files:", the case will be fail, ma jin will check it.
        ImageInventory newImage4 = addImage {
            name = "image5"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid, bs2.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        def newImage5 = addImage {
            name = "newImage5"
            url = "file:///my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
            sessionId = normalSession.uuid
        } as ImageInventory

        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage1.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage2.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage3.uuid)
                .count()
        assert 2 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage4.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage5.uuid)
                .count()
    }

    void testUploadImage(){
        def originSize = 0
        def updatedSize = 2048
        env.afterSimulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { rsp ->
            rsp.size = originSize
            rsp.uploadPath = "http://localhost:7071/ceph/image/upload"
            return rsp
        }

        env.simulator(CephBackupStorageBase.GET_DOWNLOAD_PROGRESS_PATH) {
            def rsp = new CephBackupStorageBase.GetDownloadProgressRsp()
            rsp.completed = true
            rsp.size = updatedSize
            rsp.installPath = "dummy-pool/dummy-image"
            return rsp
        }

        BackupStorageInventory bs = env.inventoryByName("ceph-bk")
        ImageInventory inv = addImage {
            name = "test-upload"
            url = "upload://myimage.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        assert inv != null
        assert inv.status == ImageStatus.Downloading.toString()
        assert inv.size == originSize

        retryInSecs {
            ImageVO image = dbFindByUuid(inv.uuid, ImageVO.class)
            assert image != null
            assert image.size == updatedSize
            assert image.status == ImageStatus.Ready
        }

        BackupStorageInventory bs_now = env.inventoryByName("ceph-bk")
        assert bs.totalCapacity == bs_now.totalCapacity
        assert bs.availableCapacity == bs_now.availableCapacity + inv.actualSize
    }

    void testCreateTemplateFromVolume() {
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        def bs = env.inventoryByName("ceph-bk") as BackupStorageInventory

        def dataVol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        def image = createDataVolumeTemplateFromVolume {
            name = "vol-image"
            volumeUuid = dataVol.uuid
        } as ImageInventory

        assert Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                .select(ImageBackupStorageRefVO_.backupStorageUuid)
                .findValue() == bs.uuid
    }

    @Override
    void clean() {
        env.delete()
    }
}