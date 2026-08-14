package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.config.primaryStorage.NfsPrimaryStorage
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.storage.primary.nfs.NfsPrimaryToSftpBackupKVMBackend
/**
 * Created by xing5 on 2017/9/16.
 */
class NfsPrimaryStorageAgent extends Agent {

    NfsPrimaryStorageAgent(Simulator simulator) {
        super(simulator)
    }

    NfsPrimaryStorage find(String uuid) {
        PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, uuid).find()
        assert ps != null : "cannot find NFS primary storage[uuid:${uuid}] in DB"

        def (ip, mountPoint) = ps.url.split(":")
        def ret = simulator.sqlite.find("select * from ${NfsPrimaryStorage.class.simpleName} where ip = '${ip}' and mountPoint = '${mountPoint}'", NfsPrimaryStorage.class)

        assert ret != null : "the nfs primary storage[uuid:${uuid}] has no simulator"

        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(NfsPrimaryStorageKVMBackend.GET_VOLUME_BASE_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            rsp.path = "/some/fake/path"
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.UNMOUNT_PRIMARY_STORAGE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH) { HttpEntity<String> e ->
            def cmd = json(e, NfsPrimaryStorageKVMBackendCommands.MountCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.MountAgentResponse()

            def nfs = find(cmd.uuid)
            rsp.totalCapacity = nfs.totalCapacity
            rsp.availableCapacity = nfs.availableCapacity
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.GET_CAPACITY_PATH) { HttpEntity<String> e ->
            def cmd = json(e, NfsPrimaryStorageKVMBackendCommands.GetCapacityCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.GetCapacityResponse()

            def nfs = find(cmd.uuid)
            rsp.totalCapacity = nfs.totalCapacity
            rsp.availableCapacity = nfs.availableCapacity
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.CreateEmptyVolumeResponse()
        }

        handle(NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromSftpBackupStorageResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.PING_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.DeleteResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.MOVE_BITS_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.MoveBitsRsp()
        }

        handle(NfsPrimaryToSftpBackupKVMBackend.UPLOAD_TO_SFTP_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.UploadToSftpResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE) {
            return new NfsPrimaryStorageKVMBackendCommands.OfflineMergeSnapshotRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.CHECK_BITS_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.CheckIsBitsExistingRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.CreateTemplateFromVolumeRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.REINIT_IMAGE_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.ReInitImageRsp()
            rsp.newVolumeInstallPath = "/new/volume/install/path"
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotResponse()
            rsp.newVolumeInstallPath = "/new/volume/install/path"
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.REBASE_MERGE_SNAPSHOT_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.RebaseAndMergeSnapshotsResponse()
            rsp.size = 0
            rsp.actualSize = 0
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp()
            rsp.size = 0
            rsp.actualSize = 0
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.MERGE_SNAPSHOT_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.MergeSnapshotResponse()
            rsp.size = 0
            rsp.actualSize = 0
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            def nfs = find(cmd.uuid)
            rsp.totalCapacity = nfs.totalCapacity
            rsp.availableCapacity = nfs.availableCapacity
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH) { HttpEntity<String> e  ->
            def cmd = json(e, NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.UpdateMountPointRsp()
            def nfs = find(cmd.uuid)
            rsp.totalCapacity = nfs.totalCapacity
            rsp.availableCapacity = nfs.availableCapacity
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.CREATE_VOLUME_FROM_TEMPLATE_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.CreateRootVolumeFromTemplateResponse()
        }

        handle(NfsPrimaryStorageKVMBackend.NFS_TO_NFS_MIGRATE_BITS_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.NfsToNfsMigrateBitsRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.NFS_REBASE_VOLUME_BACKING_FILE_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.NfsRebaseVolumeBackingFileRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.NFS_PREFIX_REBASE_BACKING_FILES_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.PrefixRebaseBackingFilesRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromKVMHostRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.GetDownloadBitsFromKVMHostProgressRsp()
            rsp.totalSize = 1L
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e ->
            def cmd = json(e, NfsPrimaryStorageKVMBackendCommands.GetQcow2HashValueCmd.class)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.GetQcow2HashValueRsp()
            rsp.hashValue = cmd.installPath
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_COMMIT) {
            def rsp = new NfsPrimaryStorageKVMBackendCommands.OfflineCommitSnapshotRsp()
            rsp.actualSize = 1
            return rsp
        }

        handle(NfsPrimaryStorageKVMBackend.GET_BACKING_CHAIN_PATH) { HttpEntity<String> e ->
            return new NfsPrimaryStorageKVMBackendCommands.GetBackingChainRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.WRITE_VM_METADATA_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.WriteVmMetadataRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.GET_VM_INSTANCE_METADATA_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.GetVmInstanceMetadataRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.SCAN_VM_METADATA_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.ScanVmMetadataRsp()
        }

        handle(NfsPrimaryStorageKVMBackend.CLEANUP_VM_METADATA_PATH) {
            return new NfsPrimaryStorageKVMBackendCommands.CleanupVmMetadataRsp()
        }
    }
}
