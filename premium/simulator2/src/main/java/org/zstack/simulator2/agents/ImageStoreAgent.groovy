package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.compute.host.MevocoKVMAgentCommands
import org.zstack.compute.host.MevocoKVMConstant
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.VolumeTO
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.backupStorage.BackupStorage
import org.zstack.simulator2.config.backupStorage.ImageStoreBackupStorage
import org.zstack.storage.backup.VolumeBackupKvmCommands
import org.zstack.storage.backup.VolumeBackupMetadataMaker
import org.zstack.storage.backup.imagestore.*
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.imagestore.ceph.CephPrimaryStorageImageStoreBackend
import org.zstack.storage.primary.imagestore.ceph.CephPrimaryToImageStoreBackupStorageMediatorImpl
import org.zstack.storage.primary.imagestore.local.LocalStorageImageStoreKvmBackend
import org.zstack.storage.primary.imagestore.local.LocalStorageKvmImageStoreBackupStorageMediatorImpl
import org.zstack.storage.primary.imagestore.nfs.NfsPrimaryStorageImageStoreBackend
import org.zstack.storage.primary.imagestore.nfs.NfsPrimaryToImageStoreBackupKVMBackend
import org.zstack.storage.primary.imagestore.smp.ImageStoreBackupStorageKvmDownloader
import org.zstack.storage.primary.imagestore.smp.ImageStoreBackupStorageKvmUploader
import org.zstack.storage.primary.imagestore.smp.SMPImageStoreKvmBackend
import org.zstack.storage.primary.smp.KvmBackend

import org.zstack.utils.SHAUtils
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by xing5 on 2017/9/27.
 */
class ImageStoreAgent extends Agent {
    ImageStoreAgent(Simulator simulator) {
        super(simulator)

        ImageStoreBackupStorageGlobalProperty.AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    BackupStorage find(String uuid) {
        ImageStoreBackupStorageVO vo = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.uuid, uuid).find()
        assert vo != null : "cannot find image store[uuid:${uuid}] in DB"

        def ret = simulator.sqlite.find("select * from ${ImageStoreBackupStorage.class.simpleName} where ip = '${vo.hostname}'", ImageStoreBackupStorage.class)
        assert ret != null : "the image store[ip:${vo.hostname}] has no simulator"
        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(ImageStoreBackupStorageConstant.CONNECT_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.ConnectCmd.class)
            def s = find(cmd.uuid)

            def rsp = new ImageStoreBackupStorageCommands.ConnectResponse()
            rsp.totalSize = s.totalCapacity
            rsp.freeSize = s.availableCapacity

            return rsp
        }

        handle(ImageStoreBackupStorageConstant.ECHO_PATH) {
            return [:]
        }

        handle(ImageStoreBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.DownloadImgCmd.class)
            def rsp = new ImageStoreBackupStorageCommands.DownloadImgResponse()
            rsp.virtualsize = 21474836480
            rsp.diskSize = 10737418240
            rsp.blobsum = SHAUtils.encrypt(cmd.imgurl, "SHA-256")
            rsp.totalSize = 1073741824000
            rsp.name = cmd.imageuuid
            rsp.id = SHAUtils.encrypt(cmd.imgurl, "SHA-1")
            rsp.freeSize = 1073741824000
            return rsp
        }

        handle(ImageStoreBackupStorageKvmDownloader.DOWNLOAD_BIT_PATH) {
            return new KvmBackend.AgentRsp()
        }

        handle(ImageStoreBackupStorageKvmUploader.UPLOAD_BIT_PATH) {
            def rsp = new ImageStoreBackupStorageKvmUploader.UploadToImageStoreResponse()
            rsp.backupStorageInstallPath = "zstore://test-image/" + Platform.getUuid()
            return rsp
        }

        handle(SMPImageStoreKvmBackend.COMMIT_PATH) {
            def rsp = new SMPImageStoreKvmBackend.CommitVolumeAsImageRsp()
            rsp.backupStorageInstallPath = "zstore://test-image/" + Platform.getUuid()
            return rsp
        }

        handle(SMPImageStoreKvmBackend.CLEAN_IMAGE_META_PATH) {
            def rsp = new SMPImageStoreKvmBackend.AgentRsp()
            return rsp
        }

        handle(LocalStorageImageStoreKvmBackend.CLEAN_IMAGE_META_PATH) {
            def rsp = new LocalStorageImageStoreKvmBackend.AgentResponse()
            return rsp
        }

        handle(LocalStorageImageStoreKvmBackend.COMMIT_PATH) {
            def rsp = new LocalStorageImageStoreKvmBackend.CommitVolumeAsImageRsp()
            rsp.backupStorageInstallPath = "zstore://test-image/" + Platform.getUuid()
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.EXPORT_IMAGE_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.ExportImageResponse()
            rsp.imgUrl = "http://localhost/test-image/deafbeef"
            rsp.md5Sum = "fake-blob-sum"
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.GET_IMAGE_INFO) {
            def rsp = new ImageStoreBackupStorageCommands.ImageInfoResponse()
            rsp.size = 10737418240
            rsp.virtualsize = 21474836480
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.DELEXP_IMAGE_PATH) {
            return new ImageStoreBackupStorageCommands.ImageStoreResponse()
        }

        handle(VolumeBackupMetadataMaker.CHECK_BACKUP_METADATA_FILE) {
            def rsp = new VolumeBackupMetadataMaker.CheckBackupMetadataFileRsp()
            rsp.exist = true
            return rsp
        }

        handle(VolumeBackupMetadataMaker.SYNC_CHECK_BACKUP_METADATA_FILE) {
            def rsp = new VolumeBackupMetadataMaker.CheckBackupMetadataFileRsp()
            rsp.exist = true
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.PING_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.PingCmd.class)
            def rsp = new ImageStoreBackupStorageCommands.PingResponse()
            rsp.uuid = cmd.uuid
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.SYNC_PING_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.PingCmd.class)
            def rsp = new ImageStoreBackupStorageCommands.PingResponse()
            rsp.uuid = cmd.uuid
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.RUNGC_IMAGE_PATH) {
            return new ImageStoreBackupStorageCommands.RunGarbageCollectorResponse()
        }

        handle(ImageStoreBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE) {
            def rsp = new ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp()
            rsp.backupStorageMetaFileName = "bs_file_info.json"
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE) {
            return new ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp()
        }

        handle(ImageStoreBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST) {
            def rsp = new ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp()
            rsp.exist = false
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.DELETE_IMAGES_METADATA) {
            def rsp  = new ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp()
            rsp.out = "delete success"
            rsp.ret = 0
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.GET_IMAGES_METADATA) {
            def rsp = new ImageStoreBackupStorageCommands.GetImagesMetaDataRsp()
            rsp.setImagesMetaData("{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}");
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.ALLOCATE_UPLOAD_DIR) {
            def rsp = new ImageStoreBackupStorageCommands.AllocateUploadSpaceResponse()
            rsp.setSuccess(true)
            rsp.setUploadDir("/upload/dir")
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.EXPORT_NBD_IMAGE) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.ExportNbdImagesCmd.class)
            def rsp = new ImageStoreBackupStorageCommands.ExportNbdImagesRsp()
            rsp.ports = (1001.. cmd.sizes.size()+1000)
            rsp.imagePaths = [cmd.workspace + "/" + Platform.uuid + ".qcow2"] * rsp.ports.size()
            rsp.nbdDescription = "backup." + Platform.uuid
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.CANCEL_EXPORT_NBD_IMAGE) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.CancelExportNbdImagesCmd.class)
            assert cmd.nbdDescription != null
            def rsp = new ImageStoreBackupStorageCommands.CancelExportNbdImagesRsp()
            return rsp
        }

	    handle(VolumeBackupKvmCommands.TAKE_VOLUMES_BACKUP_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, VolumeBackupKvmCommands.TakeBackupsCmd.class)
            def rsp = new VolumeBackupKvmCommands.TakeBackupsResponse()
            rsp.backupInfos = ([])

            for (VolumeTO volume : cmd.volumes) {
                def info = new VolumeBackupKvmCommands.VolumeBackupInfo()
                if (volume.installPath.contains("rootVolumes")) {
                    info.backupFile = "/this/root/backup"
                    info.parentInstallPath = "zstore://this/root/parent"
                } else {
                    info.backupFile = "/this/data/backup"
                    info.parentInstallPath = "zstore://this/data/parent"
                }
                info.bitmap = "bitmap"
                info.deviceId = volume.deviceId
                rsp.backupInfos.add(info)
            }
            return rsp
        }

        handle(VolumeBackupKvmCommands.TAKE_VOLUME_BACKUP_PATH) {
            def rsp = new VolumeBackupKvmCommands.TakeBackupResponse()
            rsp.bitmap ="zsbitmap"
            rsp.backupFile ="/this/root/full-back"
            rsp.parentInstallPath="zstore://this/root/parent"
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.IMPORT_BACKUP_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.ImportImageResponse()
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.INSTALL_PATH_ALLOCATE) { HttpEntity<String> e ->
            def rsp = new ImageStoreBackupStorageCommands.AllocateImageStoreInstallPathResponse()
            def cmd = JSONObjectUtil.toObject(e.body, ImageStoreBackupStorageCommands.AllocateImageStoreInstallPathCmd.class)
            if (cmd.parent != null) {
                rsp.installPath = String.format("zstore://%s/*", cmd.parent.replace("zstore://", "").split("/")[0])
            } else {
                rsp.installPath = String.format("zstore://%s/*", Platform.uuid)
            }
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.Get_IMAGE_CHAIN_INFO) {
            def rsp = new ImageStoreBackupStorageCommands.GetImageChainInfoResponse()
            def info = new ImageStoreImageResponse()
            info.name = "test-image"
            info.id = "image-id"
            rsp.chain = Collections.singletonList(info)
            return rsp
        }

        handle(NfsPrimaryStorageImageStoreBackend.COMMIT_PATH) {
            def rsp = new NfsPrimaryStorageImageStoreBackend.CommitVolumeAsImageRsp()
            rsp.actualSize = 0
            rsp.backupStorageInstallPath = "zstore://fake/image/path"
            rsp.size = 0
            return rsp
        }

        handle(NfsPrimaryStorageImageStoreBackend.CLEAN_IMAGE_META_PATH) {
            def rsp = new KVMAgentCommands.AgentResponse()
            return rsp
        }

        handle(NfsPrimaryToImageStoreBackupKVMBackend.DOWNLOAD_FROM_IMAGESTORE_PATH) {
            def rsp = new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreDownloadBitsRsp()
            rsp.totalCapacity = 0
            rsp.availableCapacity = 0
            return rsp
        }

        handle(NfsPrimaryToImageStoreBackupKVMBackend.UPLOAD_TO_IMAGESTORE_PATH) {
            def rsp = new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsRsp()
            rsp.totalCapacity = rsp.availableCapacity = 0
            rsp.backupStorageInstallPath = "ztore://test-image/" + Platform.getUuid()
            return rsp
        }

        handle(NfsPrimaryStorageImageStoreBackend.RESIZE_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageImageStoreBackend.ResizeVolumeCmd.class)
            def rsp = new NfsPrimaryStorageImageStoreBackend.ResizeVolumeRsp()
            rsp.setSize(cmd.getSize())
            return rsp
        }

        handle(CephPrimaryStorageImageStoreBackend.RESIZE_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageImageStoreBackend.ResizeVolumeCmd.class)
            def rsp = new CephPrimaryStorageImageStoreBackend.ResizeVolumeRsp()
            rsp.setSize(cmd.getSize())
            return rsp
        }

        handle(LocalStorageImageStoreKvmBackend.RESIZE_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, LocalStorageImageStoreKvmBackend.ResizeVolumeCmd.class)
            def rsp = new LocalStorageImageStoreKvmBackend.ResizeVolumeRsp()
            rsp.setSize(cmd.getSize())
            return rsp
        }

        handle(KvmBackend.RESIZE_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ResizeVolumeCmd.class)
            def rsp = new KvmBackend.ResizeVolumeRsp()
            rsp.setSize(cmd.getSize())
            return rsp
        }

        handle(LocalStorageKvmImageStoreBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {
            return new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreDownloadBitsRsp()
        }

        handle(LocalStorageKvmImageStoreBackupStorageMediatorImpl.UPLOAD_BIT_PATH) {
            def rsp = new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsRsp()
            rsp.backupStorageInstallPath = "ztore://test-image/" + Platform.getUuid()
            return rsp
        }

        handle(MevocoKVMConstant.SET_NIC_QOS) {
            def rsp = new KVMAgentCommands.AgentResponse()
            return rsp
        }

        handle(MevocoKVMConstant.GET_NIC_QOS) {
            def rsp = new MevocoKVMAgentCommands.NicAgentResponse()
            return rsp
        }

        handle(MevocoKVMConstant.SET_VOLUME_BANDWIDTH) {
            def rsp = new MevocoKVMAgentCommands.VolumeAgentResponse()
            return rsp
        }

        handle(MevocoKVMConstant.DELETE_VOLUME_BANDWIDTH) {
            def rsp = new MevocoKVMAgentCommands.VolumeAgentResponse()
            return rsp
        }

        handle(MevocoKVMConstant.GET_VOLUME_BANDWIDTH) {
            def rsp = new MevocoKVMAgentCommands.VolumeAgentResponse()
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.CEPH_IMAGESTORE_DOWNLOAD_PATH) {
            def rsp = new CephPrimaryStorageBase.AgentResponse()
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.CEPH_IMAGESTORE_COMMIT_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), CephPrimaryToImageStoreBackupStorageMediatorImpl.UploadCmd.class)
            def rsp = new CephPrimaryStorageBase.CpRsp()
            rsp.installPath = "/ceph-pri/" + cmd.imageUuid
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.CLEAN_IMAGESTORE_LOCAL_CACHE) {
            return new ImageStoreBackupStorageCommands.CleanLocalImageStoreCacheRsp()
        }

        handle(ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.DownloadFileResponse()
            rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e"
            rsp.size = 3L * 1024 * 1024 * 1024
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.FILE_UPLOAD_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.UploadFileResponse()
            rsp.directUploadUrl = "http://127.0.0.1:7761/imagestore/file/direct/upload"
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PROGRESS_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.GetDownloadFileProgressResponse()
            rsp.completed = true
            rsp.progress = 100
            rsp.size = 3L * 1024 * 1024 * 1024
            rsp.actualSize = 3L * 1024 * 1024 * 1024
            rsp.installPath = "/tmp/test-software-package/unzipInstallPath"
            rsp.format = "qcow2"
            rsp.lastOpTime = System.currentTimeMillis()
            rsp.downloadSize = 3L * 1024 * 1024 * 1024
            rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e"
            rsp.supportSuspend = true
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.FILE_CANCEL_PATH) {
            return new ImageStoreBackupStorageCommands.CancelDownloadFileRsp()
        }

        handle(ImageStoreBackupStorageConstant.DELETE_FILES_PATH) {
            return new ImageStoreBackupStorageCommands.DeleteFilesResponse()
        }

        handle(ImageStoreBackupStorageConstant.UNZIP_FILE_PATH) {
            def rsp = new ImageStoreBackupStorageCommands.UnzipFileResponse()
            rsp.unzipInstallPath = "/tmp/test-software-package/unzipInstallPath"
            rsp.fileSizes = [:]
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/Gateway_Linux_Server.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Linux.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Windows.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/TrekerInstallation.tar.gz", 1024)
            return rsp
        }

        handle(ImageStoreBackupStorageConstant.SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH) {
            return new ImageStoreBackupStorageCommands.SoftwareUpgradePackageResponse()
        }
    }
}
