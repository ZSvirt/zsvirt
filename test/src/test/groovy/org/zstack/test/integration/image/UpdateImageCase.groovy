package org.zstack.test.integration.image

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.image.ImageArchitecture
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageVO
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase

/**
 * Created by xingwei.yu on 2020-11-12.
 */
class UpdateImageCase extends SubCase{
    EnvSpec env
    CloudBus bus
    BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneSftpEnv
    }

    @Override
    void test() {
        env.create {
            bs = env.inventoryByName("sftp") as BackupStorageInventory
            testAddImageBootMode()
            testSetImageBootMode()
            testUpdateImageBootMode()
            testUpdateVRouterImage()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testAddImageBootMode() {
        def image1 = addImage {
            name = "test-image1"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
            architecture = ImageArchitecture.aarch64.toString()
            systemTags = ["bootMode::UEFI"]
        } as ImageInventory

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image1.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::UEFI"

        def image2 = addImage {
            name = "test-image2"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
            architecture = ImageArchitecture.aarch64.toString()
            systemTags = ["bootMode::Legacy"]
        } as ImageInventory

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image2.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::UEFI"

        def image3 = addImage {
            name = "test-image3"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
            architecture = ImageArchitecture.aarch64.toString()
        } as ImageInventory

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image3.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::UEFI"
    }

    void testSetImageBootMode(){
        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        updateImage {
            uuid = thisImageUuid
            architecture = "aarch64"
        }
        ImageVO vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.getArchitecture() == ImageArchitecture.aarch64.toString()

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${thisImageUuid}",
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1
    }

    void testUpdateImageBootMode(){

        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        expect(AssertionError.class){
            setImageBootMode {
                uuid = thisImageUuid
                bootMode = "Legacy"
            }
        }

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${thisImageUuid}",
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

    }

    void testUpdateVRouterImage(){

        def image4 = addImage {
            name = "test-image4"
            url = "http://my-site/vpc.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
            architecture = ImageArchitecture.aarch64.toString()
            system = true
            systemTags = ["applianceType::vrouter"]
        } as ImageInventory

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image4.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::UEFI"

        updateImage {
            uuid = image4.uuid
            architecture = "x86_64"
        }

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image4.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::Legacy"

        updateImage {
            uuid = image4.uuid
            architecture = "aarch64"
        }

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, image4.uuid)
                .like(SystemTagVO_.tag, "bootMode%")
                .select(SystemTagVO_.tag)
                .findValue() == "bootMode::UEFI"
    }
}
