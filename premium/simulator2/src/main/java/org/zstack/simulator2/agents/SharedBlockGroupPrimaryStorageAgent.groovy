package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.config.primaryStorage.BlockDevice
import org.zstack.simulator2.config.primaryStorage.SharedBlockGroupPrimaryStorage
import org.zstack.storage.primary.sharedblock.BlockDeviceStruct
import org.zstack.storage.primary.sharedblock.ImageStoreBackupStorageKvmDownloader
import org.zstack.storage.primary.sharedblock.ImageStoreBackupStorageKvmUploader
import org.zstack.storage.primary.sharedblock.SharedBlockImageStoreKvmBackend
import org.zstack.storage.primary.sharedblock.SharedBlockKvmCommands
import org.zstack.utils.SizeUtils
import org.zstack.utils.gson.JSONObjectUtil

class SharedBlockGroupPrimaryStorageAgent extends Agent {
    SharedBlockGroupPrimaryStorageAgent(Simulator simulator) {
        super(simulator)
    }

    private SharedBlockGroupPrimaryStorage find(String uuid) {
        PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, uuid).find()
        assert ps != null : "cannot find shared block group primary storage[uuid:${uuid}] in database"

        def ret = simulator.sqlite.find("select * from ${SharedBlockGroupPrimaryStorage.class.simpleName} where path = '${ps.url}'", SharedBlockGroupPrimaryStorage.class)

        if (ret == null) {
            simulator.sqlite.execute("insert into ${SharedBlockGroupPrimaryStorage.class.simpleName} (`path`, `sharedBlockGroupType`, `totalCapacity`, `availableCapacity`) VALUES ('${ps.url}', 'LvmVolumeGroupBasic', '${SizeUtils.sizeStringToBytes("10T")}', '${SizeUtils.sizeStringToBytes("10T")}')")
            ret = simulator.sqlite.find("select * from ${SharedBlockGroupPrimaryStorage.class.simpleName} where path = '${ps.url}'", SharedBlockGroupPrimaryStorage.class)
        }

        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(SharedBlockKvmCommands.PING_PATH) {HttpEntity<String> e ->
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.CONNECT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, SharedBlockKvmCommands.ConnectCmd.class)
            def ps = find(cmd.vgUuid)

            def rsp = new SharedBlockKvmCommands.ConnectRsp()
            rsp.totalCapacity = ps.totalCapacity
            rsp.availableCapacity = ps.availableCapacity
            return rsp
        }

        handle(SharedBlockKvmCommands.TAKEOVER_PATH) { HttpEntity<String> e ->
            return new SharedBlockKvmCommands.TakeoverRsp()
        }

        handle(SharedBlockKvmCommands.CREATE_VOLUME_FROM_CACHE_PATH) {
            return new SharedBlockKvmCommands.CreateVolumeFromCacheRsp()
        }

        handle(SharedBlockKvmCommands.DELETE_BITS_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.REVERT_VOLUME_FROM_SNAPSHOT_PATH) {
            def rsp = new SharedBlockKvmCommands.RevertVolumeFromSnapshotRsp()
            rsp.newVolumeInstallPath = "/new/path"
            return rsp
        }

        handle(SharedBlockKvmCommands.MERGE_SNAPSHOT_PATH) {
            return new SharedBlockKvmCommands.MergeSnapshotRsp()
        }

        handle(SharedBlockKvmCommands.GET_VOLUME_SIZE_PATH) {
            def rsp = new SharedBlockKvmCommands.GetVolumeSizeRsp()
            rsp.actualSize = 0
            rsp.size = 0
            return rsp
        }

        handle(SharedBlockKvmCommands.OFFLINE_MERGE_SNAPSHOT_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.OFFLINE_COMMIT_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new SharedBlockKvmCommands.OfflineCommitSnapshotRsp()
            rsp.actualSize = 1
            return rsp
        }

        handle(SharedBlockKvmCommands.EXTEND_MERGE_TARGET_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.EXTEND_LOGICAL_VOLUME_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }


        handle(SharedBlockKvmCommands.CREATE_EMPTY_VOLUME_PATH) {
            return new SharedBlockKvmCommands.CreateEmptyVolumeRsp()
        }

        handle(SharedBlockKvmCommands.CHECK_BITS_PATH){
            def rsp = new SharedBlockKvmCommands.CheckBitsRsp()
            rsp.existing = true
            rsp.totalCapacity = 1099511627776
            rsp.availableCapacity = 1099511627776
            return rsp
            return rsp
        }

        handle(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.CONVERT_IMAGE_TO_VOLUME) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.ADD_SHARED_BLOCK) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.RESIZE_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, SharedBlockKvmCommands.ResizeVolumeCmd.class)
            def rsp = new SharedBlockKvmCommands.ResizeVolumeRsp()
            rsp.size = cmd.size
            return rsp
        }

        handle(ImageStoreBackupStorageKvmDownloader.DOWNLOAD_BIT_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockImageStoreKvmBackend.DELETE_LV_META_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(ImageStoreBackupStorageKvmUploader.UPLOAD_BIT_PATH) {
            def rsp = new ImageStoreBackupStorageKvmUploader.UploadToImageStoreResponse()
            rsp.setBackupStorageInstallPath("zstore://" + Platform.getUuid() + "/" + Platform.getUuid())
            return rsp
        }

        handle(SharedBlockKvmCommands.CHECK_DISKS_PATH) {
            def rsp = new SharedBlockKvmCommands.AgentRsp()
            return rsp
        }

        handle(SharedBlockKvmCommands.GET_BLOCK_DEVICES_PATH) {
            def rsp = new SharedBlockKvmCommands.GetBlockDevicesRsp()
            def rets = simulator.sqlite.query("select * from ${BlockDevice.class.simpleName}", BlockDevice.class)
            assert !rets.isEmpty()

            List<BlockDeviceStruct> list = []
            for (BlockDevice bd : rets) {
                BlockDeviceStruct struct = new BlockDeviceStruct()
                struct.wwn = bd.wwn
                struct.hctl = bd.hctl
                struct.model = bd.model
                struct.vendor = bd.vendor
                struct.type = bd.type
                struct.serial = bd.serial
                struct.size = bd.size
                struct.wwid = bd.wwids
                list.add(struct)
            }

            rsp.setBlockDevices(list)

            return rsp
        }
        
        handle(SharedBlockKvmCommands.DISCONNECT_PATH) {
            def rsp = new SharedBlockKvmCommands.AgentRsp()
            return rsp
        }

        handle(SharedBlockKvmCommands.KVM_HA_SETUP_SELF_FENCER) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.KVM_HA_CANCEL_SELF_FENCER) {
            return new SharedBlockKvmCommands.AgentRsp()
        }
        handle(SharedBlockKvmCommands.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
            SharedBlockKvmCommands.DownloadBitsFromKVMHostRsp rsp = new SharedBlockKvmCommands.DownloadBitsFromKVMHostRsp()
            rsp.format = "qcow2"
            return rsp
        }

        handle(SharedBlockKvmCommands.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.GET_BACKING_CHAIN_PATH) {
            return new SharedBlockKvmCommands.GetBackingChainRsp()
        }

        handle(SharedBlockKvmCommands.CONFIG_FILTER_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.CONVERT_VOLUME_FORMAT_PATH) {
            return new SharedBlockKvmCommands.AgentRsp()
        }

        handle(SharedBlockKvmCommands.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
            SharedBlockKvmCommands.GetDownloadBitsFromKVMHostProgressRsp rsp = new SharedBlockKvmCommands.GetDownloadBitsFromKVMHostProgressRsp()
            rsp.totalSize = 1L
            return rsp
        }

        handle(SharedBlockKvmCommands.SHRINK_SNAPSHOT_PATH) {
            SharedBlockKvmCommands.ShrinkSnapshotRsp rsp = new SharedBlockKvmCommands.ShrinkSnapshotRsp()
            rsp.oldSize = 1L
            rsp.size = 1L
            return rsp
        }

        handle(SharedBlockKvmCommands.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e ->
            SharedBlockKvmCommands.GetQcow2HashValueCmd cmd = json(e, SharedBlockKvmCommands.GetQcow2HashValueCmd.class)
            SharedBlockKvmCommands.GetQcow2HashValueRsp rsp = new SharedBlockKvmCommands.GetQcow2HashValueRsp()
            rsp.hashValue = cmd.installPath
            return rsp
        }

        handle(SharedBlockKvmCommands.VGS_ALL_PATH) { HttpEntity<String> e ->
            SharedBlockKvmCommands.GetVgsInfoRsp rsp = new SharedBlockKvmCommands.GetVgsInfoRsp()
            return rsp
        }

        handle(SharedBlockKvmCommands.VGS_MANAGED_PATH) { HttpEntity<String> e ->
            SharedBlockKvmCommands.GetManagedVgsInfoRsp rsp = new SharedBlockKvmCommands.GetManagedVgsInfoRsp()
            return rsp
        }

        handle(SharedBlockKvmCommands.PREFIX_REBASE_BACKING_FILES_PATH) {
            return new SharedBlockKvmCommands.PrefixRebaseBackingFilesRsp()
        }

        handle(SharedBlockKvmCommands.WRITE_VM_METADATA_PATH) {
            return new SharedBlockKvmCommands.WriteVmMetadataRsp()
        }

        handle(SharedBlockKvmCommands.GET_VM_INSTANCE_METADATA_PATH) {
            return new SharedBlockKvmCommands.GetVmInstanceMetadataRsp()
        }

        handle(SharedBlockKvmCommands.SCAN_VM_METADATA_PATH) {
            return new SharedBlockKvmCommands.ScanVmMetadataRsp()
        }

        handle(SharedBlockKvmCommands.CLEANUP_VM_METADATA_PATH) {
            return new SharedBlockKvmCommands.CleanupVmMetadataRsp()
        }
    }
}
