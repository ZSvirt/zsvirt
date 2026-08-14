package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.config.primaryStorage.LocalStorage
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl

/**
 * Created by xing5 on 2017/9/19.
 */
class LocalStorageAgent extends Agent {
    LocalStorageAgent(Simulator simulator) {
        super(simulator)
    }

    private LocalStorage find(String uuid) {
        PrimaryStorageVO vo = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, uuid).find()
        assert vo != null: "cannot find local storage[uuid:${uuid}] in database"

        String sql = "select * from ${LocalStorage.class.simpleName} where path = '${vo.getUrl()}'"

        boolean isUrlUnique = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.url, vo.url).count() == 1
        if (!isUrlUnique) {
            sql = sql + " and name = '${vo.name}'"
        }

        def local = simulator.sqlite.find(sql, LocalStorage.class)
        assert local != null: "local storage[path:${vo.getUrl()}, name:${vo.name}] has no simulator"
        return local
    }

    @Override
    void setupAgentHandler() {
        handle(LocalStorageKvmBackend.GET_QCOW2_REFERENCE) {
            return new LocalStorageKvmBackend.GetQCOW2ReferenceRsp()
        }

        handle(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            rsp.path = "/some/patch"
            return rsp
        }

        handle(LocalStorageKvmBackend.GET_BACKING_FILE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetBackingFileRsp()
            rsp.backingFilePath = "/some/path"
            rsp.size = 0
            return rsp
        }

        handle(LocalStorageKvmBackend.GET_MD5_PATH) {
            def rsp = new LocalStorageKvmBackend.GetMd5Rsp()
            rsp.md5s = []
            return rsp
        }

        handle(LocalStorageKvmBackend.CHECK_MD5_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        handle(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { HttpEntity<String> e ->
            def cmd = json(e, LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd.class)
            def rsp = new LocalStorageKvmBackend.AgentResponse()
            if (cmd.volumeUuid == null || cmd.uuid == cmd.volumeUuid) {
                rsp.success = false
            }
            return rsp
        }

        handle(LocalStorageKvmMigrateVmFlow.REBASE_SNAPSHOT_BACKING_FILES_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        handle(LocalStorageKvmMigrateVmFlow.VERIFY_SNAPSHOT_CHAIN_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        handle(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, LocalStorageKvmBackend.InitCmd.class)
            def local = find(cmd.uuid)

            def rsp = new LocalStorageKvmBackend.InitRsp()
            rsp.totalCapacity = local.totalCapacity
            rsp.availableCapacity = local.availableCapacity
            rsp.localStorageUsedCapacity = 0
            return rsp
        }

        handle(LocalStorageKvmBackend.CHECK_BITS_PATH) {
            def rsp = new LocalStorageKvmBackend.CheckBitsRsp()
            rsp.existing = true
            return rsp
        }

        handle(LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH) { HttpEntity<String> e ->
            def cmd = json(e, LocalStorageKvmBackend.GetPhysicalCapacityCmd.class)
            def local = find(cmd.uuid)

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = local.totalCapacity
            rsp.availableCapacity = local.availableCapacity
            return rsp
        }

        handle(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            return new LocalStorageKvmBackend.CreateEmptyVolumeRsp()
        }

        handle(LocalStorageKvmBackend.CREATE_FOLDER_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        handle(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            return new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
        }

        handle(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            return new LocalStorageKvmBackend.DeleteBitsRsp()
        }

        handle(LocalStorageKvmBackend.DELETE_DIR_PATH) {
            return new LocalStorageKvmBackend.DeleteBitsRsp()
        }

        handle(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {
            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
        }

        handle(LocalStorageKvmSftpBackupStorageMediatorImpl.UPLOAD_BIT_PATH) {
            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsRsp()
        }

        handle(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME) {
            return new LocalStorageKvmBackend.CreateTemplateFromVolumeRsp()
        }

        handle(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) {
            def rsp = new LocalStorageKvmBackend.RevertVolumeFromSnapshotRsp()
            rsp.newVolumeInstallPath = "/new/snapshot/install/path"
            return rsp
        }

        handle(LocalStorageKvmBackend.REINIT_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.ReinitImageRsp()
            rsp.newVolumeInstallPath = "/new/snapshot/install/path"
            return rsp
        }

        handle(LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH) {
            return new LocalStorageKvmBackend.MergeSnapshotRsp()
        }

        handle(LocalStorageKvmBackend.GET_VOLUME_SIZE) { HttpEntity<String> e ->
            LocalStorageKvmBackend.GetVolumeSizeCmd cmd = json(e, LocalStorageKvmBackend.GetVolumeSizeCmd.class)
            LocalStorageKvmBackend.GetVolumeSizeRsp rsp = new LocalStorageKvmBackend.GetVolumeSizeRsp()
            VolumeVO volumeVO = Q.New(VolumeVO.class)
                    .eq(VolumeVO_.uuid, cmd.volumeUuid)
                    .eq(VolumeVO_.installPath, cmd.installPath)
                    .find()
            rsp.size = volumeVO.size
            rsp.actualSize = volumeVO.actualSize
            return rsp
        }

        handle(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) {
            return new LocalStorageKvmBackend.OfflineMergeSnapshotRsp()
        }

        handle(LocalStorageKvmBackend.OFFLINE_COMMIT_PATH) {
            def rsp = new LocalStorageKvmBackend.OfflineCommitSnapshotRsp()
            rsp.actualSize = 1
            return rsp
        }

        handle(LocalStorageKvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
            LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp rsp = new LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
            rsp.totalSize = 1L
            return rsp
        }

        handle(LocalStorageKvmBackend.CREATE_INITIALIZED_FILE) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        handle(LocalStorageKvmBackend.CHECK_INITIALIZED_FILE) {
            return new LocalStorageKvmBackend.CheckInitializedFileRsp()
        }

        handle(LocalStorageKvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e ->
            LocalStorageKvmBackend.GetQcow2HashValueCmd cmd = json(e, LocalStorageKvmBackend.GetQcow2HashValueCmd.class)
            LocalStorageKvmBackend.GetQcow2HashValueRsp rsp = new LocalStorageKvmBackend.GetQcow2HashValueRsp()
            rsp.hashValue = cmd.installPath
            return rsp
        }

        handle(LocalStorageKvmBackend.WRITE_VM_METADATA_PATH) {
            return new LocalStorageKvmBackend.WriteVmMetadataRsp()
        }

        handle(LocalStorageKvmBackend.GET_VM_INSTANCE_METADATA_PATH) {
            return new LocalStorageKvmBackend.GetVmInstanceMetadataRsp()
        }

        handle(LocalStorageKvmBackend.SCAN_VM_METADATA_PATH) {
            return new LocalStorageKvmBackend.ScanVmMetadataRsp()
        }

        handle(LocalStorageKvmBackend.CLEANUP_VM_METADATA_PATH) {
            return new LocalStorageKvmBackend.CleanupVmMetadataRsp()
        }

        handle(LocalStorageKvmBackend.PREFIX_REBASE_BACKING_FILES_PATH) {
            return new LocalStorageKvmBackend.PrefixRebaseBackingFilesRsp()
        }
    }
}
