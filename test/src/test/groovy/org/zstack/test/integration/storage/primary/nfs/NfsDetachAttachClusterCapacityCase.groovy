package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by SyZhao on 2017/4/17.
 */
class NfsDetachAttachClusterCapacityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testDetachAttachClusterCheckCapacity()
        }
    }

    void testDetachAttachClusterCheckCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        def bs = env.inventoryByName("sftp")

        def download_image_path_invoked = false
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            download_image_path_invoked = true
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }
        assert download_image_path_invoked

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
        }

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity + image_virtual_size + image_physical_size

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
        retryInSecs(3){
            GetPrimaryStorageCapacityResult capacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert 0 == capacity.availableCapacity
            assert 0 == capacity.availablePhysicalCapacity
            assert 0 == capacity.totalCapacity
            assert 0 == capacity.totalPhysicalCapacity
        }
        //env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
        //    LocalStorageSpec lspec = spec.specByUuid(ps.uuid)

        //    def rsp = new LocalStorageKvmBackend.AgentResponse()
        //    rsp.totalCapacity = lspec.totalCapacity
        //    rsp.availableCapacity = lspec.availableCapacity
        //    return rsp
        //}

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs(2) {
            GetPrimaryStorageCapacityResult capacityResult2 = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert capacityResult.availableCapacity == capacityResult2.availableCapacity
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs(2) {
            GetPrimaryStorageCapacityResult capacityResult3 = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert capacityResult.availableCapacity == capacityResult3.availableCapacity
            assert capacityResult.availablePhysicalCapacity == capacityResult3.availablePhysicalCapacity
        }
    }
}
