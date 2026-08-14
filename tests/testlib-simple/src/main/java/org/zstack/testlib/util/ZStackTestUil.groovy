package org.zstack.testlib.util

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.AddImageAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2017/10/17.
 */
class ZStackTestUil {

    static ImageInventory addImageToSftpBackupStorage(EnvSpec env, long imageSize, long imagePhysicalSize, String bsUuid){
        def image_virtual_size = imageSize
        def image_physical_size = imagePhysicalSize
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        AddImageAction action = new AddImageAction(
                name: "sized-image",
                url: "http://my-site/foo.iso",
                backupStorageUuids: [bsUuid],
                format: ImageConstant.QCOW2_FORMAT_STRING,
                mediaType: "RootVolumeTemplate",
                sessionId: Test.currentEnvSpec.session.uuid
        )
        AddImageAction.Result result = action.call()
        assert null == result.error
        assert null != result.value.inventory

        return result.value.inventory
    }

    static ImageInventory addImageToSftpBackupStorage(EnvSpec env, long imageSize, long imagePhysicalSize, AddImageAction action){
        def image_virtual_size = imageSize
        def image_physical_size = imagePhysicalSize
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        AddImageAction.Result result = action.call()
        assert null == result.error
        assert null != result.value.inventory

        return result.value.inventory
    }

    static void checkVmRootDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 0
        for(VolumeInventory disk : vm.allVolumes){
            if(disk.uuid == vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
                return
            }
        }
        assert false
    }

    static void  checkVmDataDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 1
        for(VolumeInventory disk : vm.allVolumes){
            if(disk.uuid != vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
            }
        }
    }
}
