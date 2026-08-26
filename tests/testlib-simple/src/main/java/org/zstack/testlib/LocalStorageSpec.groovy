package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostSystemTags
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.VFSFile
import org.zstack.testlib.vfs.Volume
import org.zstack.utils.gson.JSONObjectUtil

import java.nio.file.Path
/**
 * Created by xing5 on 2017/2/20.
 */
class LocalStorageSpec extends PrimaryStorageSpec {

    LocalStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static String hostUuidFromHTTPHeaders(HttpEntity<String> e) {
        return e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
    }

    static VFS vfs(HttpEntity<String> e, LocalStorageKvmBackend.AgentCommand cmd, EnvSpec spec) {
        return vfs(hostUuidFromHTTPHeaders(e), cmd.storagePath, spec)
    }

    static VFS vfs(String hostUuid, String storagePath, EnvSpec spec, boolean errorOnNotExisting=false) {
        return spec.getVirtualFileSystem("${hostUuid}${storagePath}", errorOnNotExisting)
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }
            
            simulator(LocalStorageKvmBackend.GET_QCOW2_REFERENCE) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.GetQCOW2ReferenceRsp(referencePaths: [])
            }

            VFS.vfsHook(LocalStorageKvmBackend.GET_QCOW2_REFERENCE, espec) {rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetQCOW2ReferenceCmd.class)

                vfs(e, cmd, spec).walkFileSystem { f ->
                    if (!(f instanceof Qcow2)) {
                        return
                    }

                    if (f.backingFile != null && f.backingFile.toAbsolutePath().toString() == cmd.path) {
                        rsp.referencePaths.add(f.pathString())
                    }
                }

                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
                def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH, espec) { LocalStorageKvmBackend.GetVolumeBaseImagePathRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetVolumeBaseImagePathCmd.class)

                VFS vfs = vfs(e, cmd, spec)

                Qcow2 qfile = vfs.getFile(cmd.volumeInstallPath)
                if (qfile == null) {
                    logger.debug("Dump of whole VFS:\\n${vfs.dumpAsString()}")
                }
                assert qfile != null : "cannot find file[${cmd.volumeInstallPath}]"
                while (qfile.backingFile != null) {
                    if (qfile.backingFile.toAbsolutePath().toString().startsWith(cmd.imageCacheDir)) {
                        rsp.path = qfile.backingFile.toAbsolutePath().toString()
                        rsp.size = qfile.backingQcow2().actualSize
                        break
                    }

                    qfile = qfile.backingQcow2()
                }

                rsp.otherPaths = []
                vfs.walkFileSystem { vfile ->
                    if (vfile.pathString().contains(cmd.volumeInstallDir)) {
                        qfile = vfile as Qcow2
                        if (qfile.backingFile != null && qfile.backingFile.toAbsolutePath().toString().startsWith(cmd.imageCacheDir)) {
                            rsp.otherPaths.add(qfile.backingFile.toAbsolutePath().toString())
                        }
                    }
                }

                rsp.otherPaths.remove(rsp.path)
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_BACKING_FILE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.GetBackingFileRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.GET_BACKING_FILE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetBackingFileCmd.class)

                Qcow2 file = vfs(e, cmd, spec).findFile { it.pathString() == cmd.path }
                assert file : "cannot find file[${cmd.path}]"

                if (file.backingFile != null) {
                    rsp.backingFilePath = file.backingFile.toAbsolutePath().toString()
                }
                rsp.size = 0
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_BACKING_CHAIN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.GetBackingChainRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.GET_BACKING_CHAIN_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetBackingChainCmd.class)

                List<String> chain = []
                VFS vfs = vfs(e, cmd, spec)
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

            simulator(LocalStorageKvmBackend.GET_MD5_PATH) {HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetMd5Cmd.class)
                def rsp = new LocalStorageKvmBackend.GetMd5Rsp()
                rsp.md5s = []
                cmd.md5s.forEach{it ->
                    def t = new LocalStorageKvmBackend.Md5TO()
                    t.resourceUuid = it.resourceUuid
                    t.path = it .path
                    t.md5 = "mockmd5" + it.resourceUuid.substring(7)
                    rsp.md5s.add(t)
                }
                return rsp
            }

            simulator(LocalStorageKvmBackend.CHECK_MD5_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.AgentResponse()
            }

            VFS.vfsHook(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd.class)
                String srcHostUuid = hostUuidFromHTTPHeaders(e)
                String storagePath = cmd.storagePath
                String dstHostUuid = Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.managementIp, cmd.dstIp).findValue()
                if (dstHostUuid == null) {
                    // try finding in HostSystemTags.EXTRA_IPS_TOKEN, this happens if migration network used
                    String tag = HostSystemTags.EXTRA_IPS.instantiateTag([(HostSystemTags.EXTRA_IPS_TOKEN) : cmd.dstIp])
                    SystemTagVO tvo = Q.New(SystemTagVO.class).eq(SystemTagVO_.tag, tag).find()
                    if (tvo != null) {
                        dstHostUuid = tvo.resourceUuid
                    }
                }

                assert dstHostUuid : "cannot find host[ip:${cmd.dstIp}]"

                VFS srcVFS = vfs(srcHostUuid, storagePath, spec)
                List<VFSFile> files = []
                cmd.paths.each {
                    VFSFile f = srcVFS.getFile(it, true)
                    files.add(f)
                }

                VFS dstVFS = vfs(dstHostUuid, storagePath, spec)
                files.each {
                    dstVFS.createFileFrom(it)
                }

                return rsp
            }

            simulator(LocalStorageKvmMigrateVmFlow.REBASE_SNAPSHOT_BACKING_FILES_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.AgentResponse()
            }

            VFS.vfsHook(LocalStorageKvmMigrateVmFlow.REBASE_SNAPSHOT_BACKING_FILES_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                cmd.snapshots.each {
                    Qcow2 f = vfs.getFile(it.path)
                    assert f : "cannot find file[${it.path}]"
                    if (it.parentPath != null) {
                        // the python localstorage agent does this, if parentPath is null, no rebase
                        f.rebase(it.parentPath)
                    }
                }

                return rsp
            }

            simulator(LocalStorageKvmMigrateVmFlow.VERIFY_SNAPSHOT_CHAIN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.AgentResponse()
            }

            VFS.vfsHook(LocalStorageKvmMigrateVmFlow.VERIFY_SNAPSHOT_CHAIN_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                cmd.snapshots.each {
                    Qcow2 f = vfs.getFile(it.path)
                    assert f : "cannot find file[${f.pathString()}]"

                    if (it.parentPath != null) {
                        Qcow2 p = vfs.getFile(it.parentPath)
                        assert p : "cannot find backing file[${it.parentPath}]"
                        assert f.backingFile == p.path : "epxect qcow2[${it.path}]'s backign file is ${it.parentPath}, but got ${f.backingFile.toAbsolutePath().toString()}"
                    }
                }

                return rsp
            }

            simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.InitCmd.class)

                def rsp = new LocalStorageKvmBackend.InitRsp()
                rsp.localStorageUsedCapacity = 0L

                LocalStorageSpec lspec = spec.specByUuid(cmd.uuid)
                if (lspec != null) {
                    rsp.totalCapacity = lspec.totalCapacity
                    rsp.availableCapacity = lspec.availableCapacity
                }

                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.INIT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.InitCmd.class)

                // init operation should use path to initialize vfs
                VFS vfs = vfs(hostUuidFromHTTPHeaders(e), cmd.path, spec)
                vfs.createDirectories(cmd.path)

                return rsp
            }

            simulator(LocalStorageKvmBackend.CHECK_BITS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.CheckBitsRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.CHECK_BITS_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CheckBitsCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                rsp.existing = vfs.getFile(cmd.path) != null
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetPhysicalCapacityCmd.class)
                LocalStorageSpec lspec = spec.specByUuid(cmd.uuid)
                assert lspec != null: "cannot find local storage[uuid:${cmd.uuid}]"

                def rsp = new LocalStorageKvmBackend.AgentResponse()
                rsp.totalCapacity = lspec.totalCapacity
                rsp.availableCapacity = lspec.availableCapacity
                return rsp
            }

            simulator(LocalStorageKvmBackend.CREATE_VOLUME_WITH_BACKING_PATH) {
                return new LocalStorageKvmBackend.CreateVolumeWithBackingRsp()
            }

            simulator(LocalStorageKvmBackend.ENCRYPT_VOLUME_BITS_PATH) {
                return new LocalStorageKvmBackend.EncryptVolumeBitsRsp()
            }

            simulator(LocalStorageKvmBackend.CONVERT_VOLUME_ENCRYPTION_PATH) {
                return new LocalStorageKvmBackend.ConvertVolumeEncryptionRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.CONVERT_VOLUME_ENCRYPTION_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.ConvertVolumeEncryptionCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                rsp.actualSizes = [:]
                cmd.items.each { item ->
                    Qcow2 source = vfs.getFile(item.sourceInstallPath)
                    assert source != null : "cannot find source file[${item.sourceInstallPath}]"
                    Qcow2 target = vfs.createQcow2(item.targetInstallPath, source.actualSize,
                            source.virtualSize, item.targetBackingInstallPath)
                    rsp.actualSizes[item.resourceUuid] = target.actualSize
                }
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.CREATE_VOLUME_WITH_BACKING_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateVolumeWithBackingCmd.class)
                VFS vfs = vfs(e, cmd, spec)

                Volume image = vfs.getFile(cmd.templatePathInCache, true)
                vfs.createQcow2(cmd.installPath, image.actualSize, image.virtualSize, cmd.templatePathInCache)
                return rsp
            }

            simulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.CreateEmptyVolumeRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_FOLDER_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
            }

            VFS.vfsHook(LocalStorageKvmBackend.CREATE_FOLDER_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateFolderCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                vfs.createDirectories(cmd.installUrl)
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateEmptyVolumeCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                vfs.createQcow2(cmd.installUrl, cmd.size, cmd.size, cmd.backingFile)
                return rsp
            }

            simulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
            }


            VFS.vfsHook(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateVolumeFromCacheCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Volume image = vfs.getFile(cmd.templatePathInCache, true)
                vfs.createQcow2(cmd.installUrl, image.actualSize, image.virtualSize, image.pathString())
                return rsp
            }

            simulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.DeleteBitsRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.DELETE_BITS_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.DeleteBitsCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                VFSFile file = vfs.getFile(cmd.path)
                assert file != null : "cannot find file[${cmd.path}]"
                file.delete()
                return rsp
            }

            simulator(LocalStorageKvmBackend.DELETE_DIR_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.DeleteBitsRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.DELETE_DIR_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.DeleteBitsCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                assert vfs.isDir(cmd.path) : "${cmd.path} is not a directory"
                vfs.delete(cmd.path)
                return rsp
            }

            simulator(LocalStorageKvmBackend.UNLINK_BITS_PATH) {
                return new LocalStorageKvmBackend.UnlinkBitsRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.UNLINK_BITS_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.UnlinkBitsCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                assert vfs.exists(cmd.installPath)
                vfs.unlink(cmd.installPath, cmd.onlyLinkedFile)
                return rsp
            }

//            simulator(LocalStorageKvmBackend.GET_LIST_PATH) { HttpEntity<String> e, EnvSpec spec ->
//                return new LocalStorageKvmBackend.ListPathRsp(paths: [])
//            }
//
//            VFS.vfsHook(LocalStorageKvmBackend.GET_LIST_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
//                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.ListPathCmd.class)
//                VFS vfs = vfs(e, cmd, spec)
//                Path path = vfs.getPath(cmd.path)
//                Files.list(path).forEach {
//                    rsp.paths.add(it.toAbsolutePath().toString())
//                }
//
//                return rsp
//            }

            simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
            }

            VFS.vfsHook(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                vfs.createQcow2(cmd.primaryStorageInstallPath, 0L, 0L, null)
                return rsp
            }

            simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.UPLOAD_BIT_PATH) {
                return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.CreateTemplateFromVolumeRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateTemplateFromVolumeCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 volume = vfs.getFile(cmd.volumePath)
                assert volume : "cannot find volume[${cmd.volumePath}]"
                volume.copyWithoutBackingFile(cmd.installPath)
                return rsp
            }

            simulator(LocalStorageKvmBackend.ESTIMATE_TEMPLATE_SIZE_PATH) {
                def rsp = new LocalStorageKvmBackend.EstimateTemplateSizeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.ESTIMATE_TEMPLATE_SIZE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.EstimateTemplateSizeCmd.class)
                VFS srcVFS = vfs(e, cmd, spec)
                Qcow2 qcow2 = srcVFS.getFile(cmd.volumePath)
                rsp.size = qcow2.virtualSize
                rsp.actualSize = qcow2.actualSize
                return rsp
            }

            simulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd.class)
                def rsp = new LocalStorageKvmBackend.RevertVolumeFromSnapshotRsp()
                rsp.newVolumeInstallPath = cmd.snapshotInstallPath + "/${Platform.uuid}".toString()
                rsp.newVolumeInstallPath = cmd.snapshotInstallPath + "/newpath"
            }

            simulator(LocalStorageKvmBackend.REINIT_IMAGE_PATH) {  HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.ReinitImageRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.REINIT_IMAGE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.ReinitImageCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 image = vfs.getFile(cmd.imagePath)
                assert image : "cannot find image[${cmd.imagePath}]"
                Path newVolumePath = vfs.getPath(cmd.volumePath).getParent().resolve("${Platform.uuid}.qcow2")
                vfs.createQcow2(newVolumePath.toAbsolutePath().toString(), image.actualSize, image.virtualSize, image.pathString())

                rsp.newVolumeInstallPath = newVolumePath.toAbsolutePath().toString()
                return rsp
            }

            simulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.RevertVolumeFromSnapshotRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 snapshot = vfs.getFile(cmd.snapshotInstallPath)
                assert snapshot : "cannot find snapshot[${cmd.snapshotInstallPath}]"
                Path newPath = snapshot.path.getParent().resolve("${Platform.uuid}.qcow2")
                vfs.createQcow2(newPath.toAbsolutePath().toString(), 0L, 0L, snapshot.pathString())

                rsp.newVolumeInstallPath = newPath.toAbsolutePath().toString()
                return rsp
            }

            simulator(LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new LocalStorageKvmBackend.MergeSnapshotRsp()
            }

            simulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetVolumeSizeCmd.class)
                LocalStorageKvmBackend.GetVolumeSizeRsp rsp = new LocalStorageKvmBackend.GetVolumeSizeRsp()
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.GET_VOLUME_SIZE, espec) { LocalStorageKvmBackend.GetVolumeSizeRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetVolumeSizeCmd.class)
                VFS vfs = vfs(e, cmd, spec)
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

            VFS.vfsHook(LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.MergeSnapshotCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 snapshot = vfs.getFile(cmd.snapshotInstallPath)
                assert snapshot : "cannot find snapshot[${cmd.snapshotInstallPath}]"
                vfs.createQcow2(cmd.workspaceInstallPath, 0L, 0L, null)
                rsp.actualSize = 1
                return rsp
            }

            simulator(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new LocalStorageKvmBackend.OfflineMergeSnapshotRsp()
                rsp.actualSize = 1
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.OFFLINE_MERGE_PATH, espec) { LocalStorageKvmBackend.OfflineMergeSnapshotRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.OfflineMergeSnapshotCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 dst = vfs.getFile(cmd.destPath, true)
                if (cmd.fullRebase) {
                    dst.rebase((String) null)
                } else {
                    dst.rebase(cmd.srcPath)
                }
                rsp.actualSize = 1
                return rsp
            }

            simulator(LocalStorageKvmBackend.OFFLINE_COMMIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new LocalStorageKvmBackend.OfflineCommitSnapshotRsp()
                rsp.actualSize = 1
                return rsp
            }

            VFS.vfsHook(LocalStorageKvmBackend.OFFLINE_COMMIT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.OfflineCommitSnapshotCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                Qcow2 src = vfs.getFile(cmd.top)
                Qcow2 dst = vfs.getFile(cmd.base)
                Qcow2 qcow2 = Qcow2.commit(vfs, src, dst)
                rsp.actualSize = qcow2.actualSize == 0 ? 1 : qcow2.actualSize
                return rsp
            }

            simulator(LocalStorageKvmBackend.CHECK_INITIALIZED_FILE) {
                return new LocalStorageKvmBackend.CheckInitializedFileRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_INITIALIZED_FILE) {
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                def rsp = new LocalStorageKvmBackend.DownloadBitsFromKVMHostRsp()
                rsp.format = "qcow2"
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmBackend.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
            }


            simulator(LocalStorageKvmBackend.HARD_LINK_VOLUME) {
                return new LocalStorageKvmBackend.LinkVolumeNewDirRsp()
            }

            VFS.vfsHook(LocalStorageKvmBackend.HARD_LINK_VOLUME, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.LinkVolumeNewDirCmd.class)
                VFS vfs = vfs(e, cmd, spec)
                def links = vfs.link(cmd.dstDir, cmd.srcDir)
                for (link in links) {
                    Qcow2 qf = vfs.getFile(link, true)
                    if (qf.backingFile != null) {
                        qf.rebase(qf.backingFile.toString().replace(cmd.srcDir, cmd.dstDir))
                    }
                    // TODO multi paths
                    qf.path = link
                    qf.update()
                }

                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
                LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp rsp = new LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
                rsp.totalSize = 1L
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetQcow2HashValueCmd.class)
                LocalStorageKvmBackend.GetQcow2HashValueRsp rsp = new LocalStorageKvmBackend.GetQcow2HashValueRsp()
                rsp.hashValue = cmd.installPath
                return rsp
            }

            simulator(LocalStorageKvmBackend.WRITE_VM_METADATA_PATH) {
                return new LocalStorageKvmBackend.WriteVmMetadataRsp()
            }

            simulator(LocalStorageKvmBackend.GET_VM_INSTANCE_METADATA_PATH) {
                return new LocalStorageKvmBackend.GetVmInstanceMetadataRsp()
            }

            simulator(LocalStorageKvmBackend.SCAN_VM_METADATA_PATH) {
                return new LocalStorageKvmBackend.ScanVmMetadataRsp()
            }

            simulator(LocalStorageKvmBackend.CLEANUP_VM_METADATA_PATH) {
                return new LocalStorageKvmBackend.CleanupVmMetadataRsp()
            }

            simulator(LocalStorageKvmBackend.PREFIX_REBASE_BACKING_FILES_PATH) {
                return new LocalStorageKvmBackend.PrefixRebaseBackingFilesRsp()
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addLocalPrimaryStorage {
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
