package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.storage.primary.smp.SftpBackupStorageKvmDownloader
import org.zstack.storage.primary.smp.SftpBackupStorageKvmUploader
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.VFSFile
import org.zstack.testlib.vfs.Volume
import org.zstack.utils.gson.JSONObjectUtil

import java.nio.file.Path
/**
 * Created by xing5 on 2017/2/20.
 */
class SharedMountPointPrimaryStorageSpec extends PrimaryStorageSpec {
    SharedMountPointPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static VFS vfs(String uuid, EnvSpec spec) {
        return spec.getVirtualFileSystem("smp-${uuid}")
    }

    static VFS vfs(KvmBackend.AgentCmd cmd, EnvSpec spec) {
        return vfs(cmd.primaryStorageUuid, spec)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(KvmBackend.CONNECT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ConnectCmd.class)
                SharedMountPointPrimaryStorageSpec sspec = spec.specByUuid(cmd.uuid)
                assert sspec != null: "cannot find shared mount point storage[uuid:${cmd.uuid}]"

                def rsp = new KvmBackend.ConnectRsp()
                rsp.totalCapacity = sspec.totalCapacity
                rsp.availableCapacity = sspec.availableCapacity
                return rsp
            }

            VFS.vfsHook(KvmBackend.CONNECT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ConnectCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                vfs.createDirectories(cmd.mountPoint)
                return rsp
            }

            simulator(KvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
                return new KvmBackend.CreateVolumeFromCacheRsp()
            }

            VFS.vfsHook(KvmBackend.CREATE_VOLUME_FROM_CACHE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.CreateVolumeFromCacheCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Volume image = vfs.getFile(cmd.templatePathInCache, true)
                vfs.createQcow2(cmd.installPath, image.actualSize, image.virtualSize, image.pathString())
                return rsp
            }

            simulator(KvmBackend.CREATE_VOLUME_WITH_BACKING_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.DELETE_BITS_PATH) {
                return new KvmBackend.DeleteRsp()
            }

            VFS.vfsHook(KvmBackend.DELETE_BITS_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.DeleteBitsCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                VFSFile file = vfs.getFile(cmd.path)
                assert file != null : "cannot find file[${cmd.path}]"
                file.delete()
                return rsp
            }

            simulator(KvmBackend.UNLINK_BITS_PATH) {
                return new KvmBackend.UnlinkBitsRsp()
            }

            VFS.vfsHook(KvmBackend.UNLINK_BITS_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.UnlinkBitsCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                assert vfs.exists(cmd.installPath)
                vfs.unlink(cmd.installPath, cmd.onlyLinkedFile)
                return rsp
            }

            simulator(KvmBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
                def rsp = new KvmBackend.CreateTemplateFromVolumeRsp()
                rsp.actualSize = 0
                rsp.size = 0
                return rsp
            }

            VFS.vfsHook(KvmBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH, xspec) { KvmBackend.CreateTemplateFromVolumeRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.CreateTemplateFromVolumeCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 volume = vfs.getFile(cmd.volumePath)
                Qcow2 template = vfs.createQcow2(cmd.installPath, volume.actualSize, volume.virtualSize)
                rsp.actualSize = template.actualSize
                rsp.size = template.virtualSize
                return rsp
            }

            simulator(KvmBackend.ESTIMATE_TEMPLATE_SIZE_PATH) {
                def rsp = new KvmBackend.EstimateTemplateSizeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(KvmBackend.ESTIMATE_TEMPLATE_SIZE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.EstimateTemplateSizeCmd.class)
                VFS srcVFS = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 qcow2 = srcVFS.getFile(cmd.volumePath)
                rsp.size = qcow2.virtualSize
                rsp.actualSize = qcow2.actualSize
                return rsp
            }

            simulator(SftpBackupStorageKvmUploader.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(SftpBackupStorageKvmDownloader.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH) {
                return new KvmBackend.AgentRsp()
            }

            VFS.vfsHook(SftpBackupStorageKvmDownloader.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.SftpDownloadBitsCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)

                String[] splitStrings = cmd.backupStorageInstallPath.split("/")
                ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, splitStrings[splitStrings.length - 2]).find()
                vfs.createQcow2(cmd.primaryStorageInstallPath, image.actualSize, image.size)

                return rsp
            }

            simulator(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.RevertVolumeFromSnapshotCmd.class)
                def rsp = new KvmBackend.RevertVolumeFromSnapshotRsp()
                rsp.newVolumeInstallPath = cmd.snapshotInstallPath + "/newpath"
                return rsp
            }

            VFS.vfsHook(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.RevertVolumeFromSnapshotCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                VFSFile sp = vfs.getFile(cmd.snapshotInstallPath)
                Path newVolumePath = sp.path.parent.resolve("${Platform.uuid}.qcow2")
                vfs.createQcow2(newVolumePath.toString(), 0L, 0L, sp.pathString())
                rsp.newVolumeInstallPath = newVolumePath.toString()
                return rsp
            }

            simulator(KvmBackend.REINIT_IMAGE_PATH) {
                def rsp = new KvmBackend.ReInitImageRsp()
                rsp.newVolumeInstallPath = "/new/path"
                return rsp
            }

            VFS.vfsHook(KvmBackend.REINIT_IMAGE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ReInitImageCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 image = vfs.getFile(cmd.imageInstallPath)
                assert image : "cannot find image[${cmd.imageInstallPath}]"
                Path newVolumePath = vfs.getPath(cmd.volumeInstallPath).getParent().resolve("${Platform.uuid}.qcow2")
                vfs.createQcow2(newVolumePath.toAbsolutePath().toString(), image.actualSize, image.virtualSize, image.pathString())
                rsp.newVolumeInstallPath = newVolumePath.toAbsolutePath().toString()
                return rsp
            }

            simulator(KvmBackend.MERGE_SNAPSHOT_PATH) {
                return new KvmBackend.MergeSnapshotRsp()
            }

            VFS.vfsHook(KvmBackend.MERGE_SNAPSHOT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.MergeSnapshotCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 snapshot = vfs.getFile(cmd.snapshotInstallPath)
                assert snapshot : "cannot find snapshot[${cmd.snapshotInstallPath}]"
                vfs.createQcow2(cmd.workspaceInstallPath, 0L, 0L, null)
                return rsp
            }

            simulator(KvmBackend.GET_VOLUME_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetVolumeSizeCmd.class)
                KvmBackend.GetVolumeSizeRsp rsp = new KvmBackend.GetVolumeSizeRsp()
                Long size = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                boolean isSnapshotExist = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, cmd.volumeUuid)
                        .exists
                if (!isSnapshotExist) {
                    rsp.actualSize = Q.New(VolumeVO.class).select(VolumeVO_.actualSize).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                } else {
                    rsp.actualSize = 1L
                }
                rsp.size = size
                return rsp
            }

            VFS.vfsHook(KvmBackend.GET_VOLUME_SIZE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetVolumeSizeCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 volume = vfs.getFile(cmd.installPath)
                rsp.size = volume.virtualSize
                rsp.actualSize = volume.actualSize

                boolean isSnapshotExist = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, cmd.volumeUuid)
                        .exists
                if (!isSnapshotExist) {
                    rsp.actualSize = Q.New(VolumeVO.class).select(VolumeVO_.actualSize).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                } else {
                    rsp.actualSize = 1L
                }
                return rsp
            }

            simulator(KvmBackend.OFFLINE_MERGE_SNAPSHOT_PATH) {
                def rsp = new KvmBackend.OfflineMergeSnapshotRsp()
                rsp.actualSize = 1
                return rsp
            }

            VFS.vfsHook(KvmBackend.OFFLINE_MERGE_SNAPSHOT_PATH, xspec) { KvmBackend.OfflineMergeSnapshotRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.OfflineMergeSnapshotCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 dst = vfs.getFile(cmd.destPath, true)
                if (cmd.fullRebase) {
                    dst.rebase((String) null)
                } else {
                    dst.rebase(cmd.srcPath)
                }
                rsp.actualSize = 1
                return rsp
            }

            simulator(KvmBackend.OFFLINE_COMMIT_SNAPSHOT_PATH) {
                def rsp = new KvmBackend.OfflineCommitSnapshotRsp()
                rsp.actualSize = 1
                return rsp
            }

            VFS.vfsHook(KvmBackend.OFFLINE_COMMIT_SNAPSHOT_PATH, xspec) { KvmBackend.OfflineCommitSnapshotRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.OfflineCommitSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                Qcow2 src = vfs.getFile(cmd.top)
                Qcow2 dst = vfs.getFile(cmd.base)
                Qcow2 qcow2 = Qcow2.commit(vfs, src, dst)
                rsp.actualSize = qcow2.actualSize == 0 ? 1 : qcow2.actualSize
                return rsp
            }

            simulator(KvmBackend.CREATE_EMPTY_VOLUME_PATH) {
                return new KvmBackend.CreateEmptyVolumeRsp()
            }

            VFS.vfsHook(KvmBackend.CREATE_EMPTY_VOLUME_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.CreateEmptyVolumeCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                vfs.createQcow2(cmd.installPath, 0L, cmd.size)
                return rsp
            }

            simulator(KvmBackend.CREATE_FOLDER_PATH) {
                return new KvmBackend.AgentRsp()
            }

            VFS.vfsHook(KvmBackend.CREATE_FOLDER_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.CreateFolderCmd.class)

                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                vfs.createDirectories(cmd.installPath)

                return rsp
            }

            simulator(KvmBackend.CHECK_BITS_PATH){
                def rsp = new KvmBackend.CheckBitsRsp()
                rsp.existing = true
                return rsp
            }

            simulator(KvmBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                def rsp = new KvmBackend.DownloadBitsFromKVMHostRsp()
                rsp.format = "qcow2"
                return rsp
            }

            simulator(KvmBackend.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.HARD_LINK_VOLUME) {
                return new KvmBackend.LinkVolumeNewDirRsp()
            }

            VFS.vfsHook(KvmBackend.HARD_LINK_VOLUME, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.LinkVolumeNewDirCmd.class)
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                def links = vfs.link(cmd.dstDir, cmd.srcDir)
                for (link in links) {
                    Qcow2 qf = vfs.getFile(link, true)
                    if (qf.backingFile != null) {
                        qf.rebase(qf.backingFile.toString().replace(cmd.srcDir, cmd.dstDir))
                    }
                }

                return rsp
            }

            simulator(KvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
                def rsp = new KvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
                rsp.totalSize = 1L
                return rsp
            }

            simulator(KvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetQcow2HashValueCmd.class)
                def rsp = new KvmBackend.GetQcow2HashValueRsp()
                rsp.hashValue = cmd.installPath
                return rsp
            }

            simulator(KvmBackend.GET_BACKING_CHAIN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new KvmBackend.GetBackingChainRsp()
            }

            VFS.vfsHook(KvmBackend.GET_BACKING_CHAIN_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetBackingChainCmd.class)

                List<String> chain = []
                VFS vfs = SharedMountPointPrimaryStorageSpec.vfs(cmd, spec)
                Qcow2 file = vfs.getFile(cmd.installPath)
                if (file == null) {
                    logger.debug("Dump of whole VFS:\\n${vfs.dumpAsString()}")
                }
                assert file != null : "cannot find file[${cmd.installPath}]"
                while (file.backingQcow2() != null) {
                    chain.add(file.backingQcow2().pathString())
                    file = file.backingQcow2()
                }

                rsp.backingChain = chain
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addSharedMountPointPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
