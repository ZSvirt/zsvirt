package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.*
import org.zstack.header.storage.backup.AllocateBackupStorageMsg
import org.zstack.header.storage.backup.AllocateBackupStorageReply
import org.zstack.header.storage.backup.BackupStorageInventory
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmMsg
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmReply
import org.zstack.header.volume.CreateDataVolumeTemplateFromDataVolumeMsg
import org.zstack.header.volume.CreateDataVolumeTemplateFromDataVolumeReply
import org.zstack.header.volume.SyncVolumeSizeMsg
import org.zstack.header.volume.SyncVolumeSizeReply
import org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit

/**
 * Created by david on 3/2/17.
 */
class ImageOperationsCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VmInstanceInventory vm
    org.zstack.sdk.BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "username"
                password = "password"
                hostname = "hostname"

                image {
                    name = "image"
                    url = "http://somehost/boot.iso"
                    format = "iso"
                }

                image {
                    name = "image1"
                    url = "http://somehost/boot.iso"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteImage()
            testDeleteImageWhichUsedInVm()
            testDeleteDownloadingImage()
            testImageBackupStorageRefVOHasInfoWhenImageDownloading()
            testCreateImageFromDataVolumeAssertHasRefVOWhenImageDownloadingDB()
            testCreateImageFromDataVolumeAssertHasRefVOWhenImageDownloadingDB2()
            testCreateImageHasImageBsRef()
            testCreateImageFailDbRollBack()
            testDeleteImageWhileCreateIt()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    class TestExpungeImageExt implements ExpungeImageExtensionPoint {
        boolean called = false
        boolean found = true

        @Override
        void preExpungeImage(ImageInventory img) {
        }

        @Override
        void beforeExpungeImage(ImageInventory img) {
        }

        @Override
        void afterExpungeImage(ImageInventory img, String imageBackupStorageUuid) {
            called = true
            found = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, img.uuid)
                    .eq(ImageBackupStorageRefVO_.backupStorageUuid, imageBackupStorageUuid)
                    .exists
        }

        @Override
        void failedToExpungeImage(ImageInventory img, ErrorCode err) {
        }
    }

    void testDeleteImage() {
        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        deleteImage {
            uuid = thisImageUuid
        }

        ImageVO vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.status == ImageStatus.Deleted
        assert vo.backupStorageRefs.size() == 1
        assert vo.backupStorageRefs[0].status == vo.status

        recoverImage {
            imageUuid = thisImageUuid
        }

        vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.status == ImageStatus.Ready
        assert vo.backupStorageRefs.size() == 1
        assert vo.backupStorageRefs[0].status == vo.status

        deleteImage {
            uuid = thisImageUuid
        }

        PluginRegistry pluginRegistry = bean(PluginRegistry.class)
        def expungeExts = pluginRegistry.getExtensionList(ExpungeImageExtensionPoint.class)
        def testExt = new TestExpungeImageExt()
        expungeExts.add(testExt)

        expungeImage {
            imageUuid = thisImageUuid
        }

        assert testExt.called
        assert !testExt.found
    }

    void testDeleteImageWhichUsedInVm() {
        org.zstack.sdk.ImageInventory usedImage = env.inventoryByName("image1")
        deleteImage {
            uuid = usedImage.uuid
        }
        expungeImage {
            imageUuid = usedImage.uuid
        }
    }

    void testDeleteDownloadingImage() {
        def bs = env.inventoryByName("sftp")

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            rsp, HttpEntity<String> e ->
                TimeUnit.SECONDS.sleep(3)
                return rsp
        }

        def imageName = "large-image"
        def large = addImage {
            name = imageName
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        retryInSecs {
            assert dbIsExists(large.uuid, ImageVO.class)
        }

        deleteImage {
            uuid = large.uuid
        }

        expungeImage {
            imageUuid = large.uuid
        }

        env.cleanSimulatorHandlers()
        assert !dbIsExists(large.uuid, ImageVO.class)

        Long cnt = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, large.uuid)
                .count()

        assert cnt == 0L
    }

    void testImageBackupStorageRefVOHasInfoWhenImageDownloading() {
        def bs = env.inventoryByName("sftp")

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            rsp, HttpEntity<String> e ->
                TimeUnit.SECONDS.sleep(3)
                return rsp
        }

        def imageName = "large-image"
        def large = addImage {
            name = imageName
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        retryInSecs {
            assert dbIsExists(large.uuid, ImageVO.class)
        }

        env.cleanSimulatorHandlers()
        ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, large.uuid).eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid).find()
        assert vo != null
        assert vo.status == ImageStatus.Ready
    }

    void testCreateImageFromDataVolumeAssertHasRefVOWhenImageDownloadingDB() {
        dbf = bean(DatabaseFacade.class)
        bs = env.inventoryByName("sftp")
        vm = env.inventoryByName("vm")
        String imageName = "test-image"

        stopVmInstance {
            uuid = vm.uuid
        }

        env.message(SyncVolumeSizeMsg.class) { SyncVolumeSizeMsg msg, CloudBus bus ->
            def reply = new SyncVolumeSizeReply()
            reply.size = 1L
            bus.reply(msg, reply)
        }

        env.message(AllocateBackupStorageMsg.class) { AllocateBackupStorageMsg msg, CloudBus bus ->
            def reply = new AllocateBackupStorageReply()
            reply.inventory = BackupStorageInventory.valueOf(dbf.findByUuid(bs.uuid, BackupStorageVO))
            bus.reply(msg, reply)
        }

        env.message(CreateDataVolumeTemplateFromDataVolumeMsg.class) { CreateDataVolumeTemplateFromDataVolumeMsg msg, CloudBus bus ->
            def reply = new CreateDataVolumeTemplateFromDataVolumeReply()
            reply.backupStorageUuid = bs.uuid
            reply.installPath = "/root/hello/data-volume/path"
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
            assert image != null
            ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                    .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid)
                    .eq(ImageBackupStorageRefVO_.status, ImageStatus.Creating)
                    .find()
            assert vo != null
            bus.reply(msg, reply)
        }

        env.message(CreateTemplateFromRootVolumeVmMsg.class) { CreateTemplateFromRootVolumeVmMsg msg, CloudBus bus ->
            def reply = new CreateTemplateFromRootVolumeVmReply()
            reply.installPath = "/root/hello/root-volume/path"
            bus.reply(msg, reply)
        }

        env.message(SyncImageSizeMsg.class) { SyncImageSizeMsg msg, CloudBus bus ->
            def reply = new SyncImageSizeReply()
            reply.size = 2L
            reply.actualSize = 1L
            bus.reply(msg, reply)
        }

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            rsp, HttpEntity<String> e ->
                return rsp
        }

        def volume = createDataVolume {
            name = "test-data-volume"
            diskSize = SizeUnit.GIGABYTE.toByte(20)
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = volume.uuid
        }

        createDataVolumeTemplateFromVolume {
            name = imageName
            volumeUuid = volume.uuid
            backupStorageUuids = [bs.uuid]
        }
    }

    void testCreateImageFromDataVolumeAssertHasRefVOWhenImageDownloadingDB2() {
        String imageName = "test-image23"
        env.message(CreateDataVolumeTemplateFromDataVolumeMsg.class) { CreateDataVolumeTemplateFromDataVolumeMsg msg, CloudBus bus ->
            def reply = new CreateDataVolumeTemplateFromDataVolumeReply()
            reply.backupStorageUuid = bs.uuid
            reply.installPath = "/root/hello/data-volume/path"
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
            assert image != null
            ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                    .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid)
                    .eq(ImageBackupStorageRefVO_.status, ImageStatus.Creating)
                    .find()
            assert vo != null
            bus.reply(msg, reply)
        }

        def volume = createDataVolume {
            name = "test-data-volume2"
            diskSize = SizeUnit.GIGABYTE.toByte(20)
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = volume.uuid
        }

        createDataVolumeTemplateFromVolume {
            name = imageName
            volumeUuid = volume.uuid
            // not choice bs
        }

    }

    void testCreateImageHasImageBsRef() {
        String imageName = "test"
        env.message(AllocateBackupStorageMsg.class) { AllocateBackupStorageMsg msg, CloudBus bus ->
            def reply = new AllocateBackupStorageReply()
            reply.inventory = BackupStorageInventory.valueOf(dbf.findByUuid(bs.uuid, BackupStorageVO))
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
            assert image != null
            bus.reply(msg, reply)
        }

        env.message(CreateTemplateFromRootVolumeVmMsg.class) { CreateTemplateFromRootVolumeVmMsg msg, CloudBus bus ->
            def reply = new CreateTemplateFromRootVolumeVmReply()
            reply.installPath = "/root/hello/root-volume/path"
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
            assert image != null
            ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                    .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid)
                    .eq(ImageBackupStorageRefVO_.status, ImageStatus.Creating)
                    .find()
            assert vo != null
            bus.reply(msg, reply)
        }
        def image = createRootVolumeTemplateFromRootVolume {
            name = imageName
            rootVolumeUuid = vm.rootVolumeUuid
        }
        ImageBackupStorageRefVO vo =  Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                .find()
        assert vo.status == ImageStatus.Ready
    }

    void testCreateImageFailDbRollBack() {
        String imageName3 = "test3"
        long beforeRefCount = Q.New(ImageBackupStorageRefVO.class).count()
        long beforeImageCount = Q.New(ImageVO.class).count()

        env.message(CreateTemplateFromRootVolumeVmMsg.class) { CreateTemplateFromRootVolumeVmMsg msg, CloudBus bus ->
            def reply = new CreateTemplateFromRootVolumeVmReply()
            reply.setError(new ErrorCode("10000", "on purpose"))
            bus.reply(msg, reply)
        }

        def a = new CreateRootVolumeTemplateFromRootVolumeAction()
        a.name = imageName3
        a.rootVolumeUuid = vm.rootVolumeUuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null

        assert Q.New(ImageBackupStorageRefVO.class).count() == beforeRefCount
        assert Q.New(ImageVO.class).count() == beforeImageCount
    }

    void testDeleteImageWhileCreateIt(){
        long beforeRefCount = Q.New(ImageBackupStorageRefVO.class).count()
        long beforeImageCount = Q.New(ImageVO.class).count()

        env.message(CreateTemplateFromRootVolumeVmMsg.class) { CreateTemplateFromRootVolumeVmMsg msg, CloudBus bus ->
            def reply = new CreateTemplateFromRootVolumeVmReply()
            reply.installPath = "/root/hello/root-volume/path"
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
            assert image != null
            ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
                    .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid)
                    .eq(ImageBackupStorageRefVO_.status, ImageStatus.Downloading)
                    .find()
            assert vo != null
            deleteImage(){
                uuid = Q.New(ImageVO.class).select(ImageVO_.uuid).eq(ImageVO_.status, ImageStatus.Downloading).findValue()
            }
            bus.reply(msg, reply)
        }

        def a = new CreateRootVolumeTemplateFromRootVolumeAction()
        a.name = "test"
        a.rootVolumeUuid = vm.rootVolumeUuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null

        assert Q.New(ImageBackupStorageRefVO.class).count() == beforeRefCount
        assert Q.New(ImageVO.class).count() == beforeImageCount
        env.cleanSimulatorHandlers()
    }

}
