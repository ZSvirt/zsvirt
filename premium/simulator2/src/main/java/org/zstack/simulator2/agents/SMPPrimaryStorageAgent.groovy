package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.config.primaryStorage.SharedMountPointPrimaryStorage
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.storage.primary.smp.SftpBackupStorageKvmDownloader
import org.zstack.storage.primary.smp.SftpBackupStorageKvmUploader

/**
 * Created by xing5 on 2017/9/19.
 */
class SMPPrimaryStorageAgent extends Agent {
    SMPPrimaryStorageAgent(Simulator simulator) {
        super(simulator)
    }

    private SharedMountPointPrimaryStorage find(String uuid) {
        PrimaryStorageVO smp = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, uuid).find()
        assert smp != null : "cannot find shared mount point primary storage[uuid:${uuid}] in database"

        def ret = simulator.sqlite.find("select * from ${SharedMountPointPrimaryStorage.class.simpleName} where path = '${smp.url}'", SharedMountPointPrimaryStorage.class)
        assert ret != null : "SMP primary storage[path:${smp.url}] has no simulator"
        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(KvmBackend.CONNECT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, KvmBackend.ConnectCmd.class)
            def smp = find(cmd.uuid)

            def rsp = new KvmBackend.ConnectRsp()
            rsp.totalCapacity = smp.totalCapacity
            rsp.availableCapacity = smp.availableCapacity
            return rsp
        }

        handle(KvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            return new KvmBackend.CreateVolumeFromCacheRsp()
        }

        handle(KvmBackend.DELETE_BITS_PATH) {
            return new KvmBackend.DeleteRsp()
        }

        handle(KvmBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
            return new KvmBackend.AgentRsp()
        }

        handle(SftpBackupStorageKvmUploader.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH) {
            return new KvmBackend.AgentRsp()
        }

        handle(SftpBackupStorageKvmDownloader.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH) {
            return new KvmBackend.AgentRsp()
        }

        handle(KvmBackend.REINIT_IMAGE_PATH) {
            def rsp = new KvmBackend.ReInitImageRsp()
            rsp.newVolumeInstallPath = "/new/path"
            return rsp
        }

        handle(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) {
            def rsp = new KvmBackend.RevertVolumeFromSnapshotRsp()
            rsp.newVolumeInstallPath = "/new/path"
            return rsp
        }

        handle(KvmBackend.MERGE_SNAPSHOT_PATH) {
            return new KvmBackend.MergeSnapshotRsp()
        }

        handle(KvmBackend.GET_VOLUME_SIZE_PATH) {
            def rsp = new KvmBackend.GetVolumeSizeRsp()
            rsp.actualSize = 0
            rsp.size = 0
            return rsp
        }

        handle(KvmBackend.OFFLINE_MERGE_SNAPSHOT_PATH) {
            return new KvmBackend.AgentRsp()
        }

        handle(KvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            return new KvmBackend.CreateEmptyVolumeRsp()
        }

        handle(KvmBackend.CHECK_BITS_PATH){
            def rsp = new KvmBackend.CheckBitsRsp()
            rsp.existing = true
            return rsp
        }

        handle(KvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
            def rsp = new KvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
            rsp.totalSize = 1L
            return rsp
        }

        handle(KvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e ->
            def cmd = new KvmBackend.GetQcow2HashValueCmd()
            def rsp = new KvmBackend.GetQcow2HashValueRsp()
            rsp.hashValue = cmd.installPath
            return rsp
        }

        handle(KvmBackend.GET_BACKING_CHAIN_PATH) { HttpEntity<String> e ->
            return new KvmBackend.GetBackingChainRsp()
        }

        handle(KvmBackend.OFFLINE_COMMIT_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new KvmBackend.OfflineCommitSnapshotRsp()
            rsp.actualSize = 1
            return rsp
        }
    }
}
