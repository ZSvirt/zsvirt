package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageStatus
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by mingjian.deng on 2017/11/24.
 * create vm and delete its image, then test start
 */
class StartVmAfterImageDeleteCase extends SubCase {
    EnvSpec env
    ImageInventory image
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            createVmWithNewImage()
            testDeleteImage()
            testStartVm()
            testMigrateVm()
            testCreateTemplateFromRootVolume()
            testGetCandidate()
            testDeleteVm()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void createVmWithNewImage() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("pubL3")

        image = addImage {
            name = "new-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        } as ImageInventory

        SQL.New(ImageVO.class)
                .eq(ImageVO_.uuid, image.uuid)
                .set(ImageVO_.status, ImageStatus.Downloading)
                .update()

        expect(AssertionError.class) {
            vm = createVmInstance {
                name = "vm"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = offering.uuid
            } as VmInstanceInventory
        }

        SQL.New(ImageVO.class)
                .eq(ImageVO_.uuid, image.uuid)
                .set(ImageVO_.status, ImageStatus.Ready)
                .update()

        vm = createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
        } as VmInstanceInventory
        assert Q.New(VmInstanceVO.class).count() == 1
    }

    void testDeleteImage() {
        deleteImage {
            delegate.uuid = image.uuid
        }

        expungeImage {
            delegate.imageUuid = image.uuid
        }
    }

    void testStartVm() {
        // start vm which image has been deleted
        vm = stopVmInstance {
            delegate.uuid = vm.uuid
        } as VmInstanceInventory
        assert vm.state == VmInstanceState.Stopped.toString()

        vm = startVmInstance {
            delegate.uuid = vm.uuid
        } as VmInstanceInventory
        assert vm.state == VmInstanceState.Running.toString()

        vm = rebootVmInstance{
            delegate.uuid = vm.uuid
        } as VmInstanceInventory
        assert vm.state == VmInstanceState.Running.toString()
    }

    void testMigrateVm(){
        def hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 0
    }

    void testCreateTemplateFromRootVolume(){
        vm = stopVmInstance {
            delegate.uuid = vm.uuid
        } as VmInstanceInventory
        assert vm.state == VmInstanceState.Stopped.toString()

        createRootVolumeTemplateFromRootVolume {
            name = "test"
            rootVolumeUuid = vm.rootVolumeUuid
        }
    }

    void testGetCandidate() {
        def result = getVmStartingCandidateClustersHosts {
            delegate.uuid = vm.uuid
        } as GetVmStartingCandidateClustersHostsResult
        assert result.hosts.size() == 1
        assert result.hosts.get(0).uuid == vm.lastHostUuid
        assert result.clusters.size() == 1
    }

    void testDeleteVm() {
        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }

        assert Q.New(VmInstanceVO.class).count() == 0
    }
}
