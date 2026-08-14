package org.zstack.storage.primary.local;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.zstack.core.thread.AsyncThread;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.*;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsRsp;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageSimulator {
    private CLogger logger = Utils.getLogger(LocalStorageSimulator.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private LocalStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;

    @AsyncThread
    public void reply(HttpEntity<String> entity, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        String rspBody = JSONObjectUtil.toJsonString(rsp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(rspBody.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers);
        restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_QCOW2_REFERENCE, method= RequestMethod.POST)
    public @ResponseBody
    String getQcow2Reference(HttpEntity<String> entity) {
        GetQCOW2ReferenceCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetQCOW2ReferenceCmd.class);
        GetQCOW2ReferenceRsp rsp = new GetQCOW2ReferenceRsp();
        config.getQCOW2ReferenceCmds.add(cmd);
        rsp.referencePaths = config.getQCOW2ReferenceCmdReference;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_BASE_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getVolumeBaseImagePath(HttpEntity<String> entity) {
        GetVolumeBaseImagePathCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeBaseImagePathCmd.class);
        GetVolumeBaseImagePathRsp rsp = new GetVolumeBaseImagePathRsp();
        rsp.path = config.getVolumeBaseImagePaths.get(cmd.volumeUuid);
        rsp.size = config.baseImageSize;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_BACKING_FILE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getBackingFile(HttpEntity<String> entity) {
        GetBackingFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetBackingFileCmd.class);
        GetBackingFileRsp rsp = new GetBackingFileRsp();
        config.getBackingFileCmds.add(cmd);
        rsp.backingFilePath = config.backingFilePath;
        rsp.size = config.backingFileSize;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_MD5_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getMd5sum(HttpEntity<String> entity) {
        GetMd5Cmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetMd5Cmd.class);
        GetMd5Rsp rsp = new GetMd5Rsp();
        config.getMd5Cmds.add(cmd);
        rsp.md5s = transformAndRemoveNull(cmd.md5s, arg -> {
            Md5TO to = new Md5TO();
            to.md5 = arg.resourceUuid;
            to.path = arg.path;
            to.resourceUuid = arg.resourceUuid;
            return to;
        });

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CHECK_MD5_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkMd5sum(HttpEntity<String> entity) {
        CheckMd5sumCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckMd5sumCmd.class);
        config.checkMd5sumCmds.add(cmd);
        AgentResponse rsp = new AgentResponse();
        if (!config.checkMd5Success) {
            rsp.setSuccess(false);
            rsp.setError("on purpose");
        }
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String copyBitsFromRemote(HttpEntity<String> entity) {
        CopyBitsFromRemoteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CopyBitsFromRemoteCmd.class);
        AgentResponse rsp = new AgentResponse();
        if (config.copyBitsFromRemoteSuccess) {
            config.copyBitsFromRemoteCmds.add(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        reply(entity, rsp);
        return null;}

    @RequestMapping(value=LocalStorageKvmMigrateVmFlow.REBASE_SNAPSHOT_BACKING_FILES_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String rebaseSnapshotBackingFiles(HttpEntity<String> entity) {
        RebaseSnapshotBackingFilesCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RebaseSnapshotBackingFilesCmd.class);
        config.rebaseSnapshotBackingFilesCmds.add(cmd);
        reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmMigrateVmFlow.VERIFY_SNAPSHOT_CHAIN_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String verifySnapshotChain(HttpEntity<String> entity) {
        VerifySnapshotChainCmd cmd = JSONObjectUtil.toObject(entity.getBody(), VerifySnapshotChainCmd.class);
        config.verifySnapshotChainCmds.add(cmd);
        reply(entity, new AgentResponse());
        return null;
    }


    @RequestMapping(value=LocalStorageKvmBackend.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String init(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        config.initCmdList.add(cmd);

        AgentResponse rsp = new AgentResponse();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.name);
        q.add(HostVO_.uuid, Op.EQ, cmd.getHostUuid());
        String hname = q.findValue();

        Capacity c = config.capacityMap.get(hname);
        assert c!=null : String.format("cannot find host[name:%s] for configuring the local storage capacity", hname);
        rsp.setTotalCapacity(c.total);
        rsp.setAvailableCapacity(c.avail);
        reply(entity, rsp);

        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CHECK_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkBits(HttpEntity<String> entity) {
        CheckBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckBitsCmd.class);
        config.checkBitsCmds.add(cmd);
        CheckBitsRsp rsp = new CheckBitsRsp();
        rsp.existing = config.checkBitsSuccess;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getPhysicalCapacity(HttpEntity<String> entity) {
        GetPhysicalCapacityCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetPhysicalCapacityCmd.class);
        config.getPhysicalCapacityCmds.add(cmd);

        AgentResponse rsp = new AgentResponse();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.name);
        q.add(HostVO_.uuid, Op.EQ, cmd.getHostUuid());
        String hname = q.findValue();

        Capacity c = config.capacityMap.get(hname);
        rsp.setTotalCapacity(c.total);
        rsp.setAvailableCapacity(c.avail);
        reply(entity, rsp);

        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        config.createEmptyVolumeCmds.add(cmd);
        CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createVolumeFromCache(HttpEntity<String> entity) {
        CreateVolumeFromCacheCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVolumeFromCacheCmd.class);
        config.createVolumeFromCacheCmds.add(cmd);
        CreateVolumeFromCacheRsp rsp = new CreateVolumeFromCacheRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.DELETE_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String delete(HttpEntity<String> entity) {
        DeleteBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteBitsCmd.class);
        synchronized (config) {
            config.deleteBitsCmds.add(cmd);
        }
        DeleteBitsRsp rsp = new DeleteBitsRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.DELETE_DIR_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deleteDir(HttpEntity<String> entity) {
        DeleteBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteBitsCmd.class);
        synchronized (config) {
            config.deleteDirCmds.add(cmd);
        }
        DeleteBitsRsp rsp = new DeleteBitsRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String download(HttpEntity<String> entity) {
        SftpDownloadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpDownloadBitsCmd.class);
        config.downloadBitsCmds.add(cmd);
        reply(entity, new SftpDownloadBitsRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmSftpBackupStorageMediatorImpl.UPLOAD_BIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String upload(HttpEntity<String> entity) {
        SftpUploadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpUploadBitsCmd.class);
        config.uploadBitsCmds.add(cmd);
        reply(entity, new SftpUploadBitsRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME, method= RequestMethod.POST)
    public @ResponseBody
    String createTemplateFromVolume(HttpEntity<String> entity) {
        CreateTemplateFromVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateTemplateFromVolumeCmd.class);
        config.createTemplateFromVolumeCmds.add(cmd);
        reply(entity, new CreateTemplateFromVolumeRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String revertSnapshot(HttpEntity<String> entity) {
        RevertVolumeFromSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RevertVolumeFromSnapshotCmd.class);
        config.revertVolumeFromSnapshotCmds.add(cmd);
        RevertVolumeFromSnapshotRsp rsp = new RevertVolumeFromSnapshotRsp();
        rsp.setNewVolumeInstallPath("/new/path");
        reply(entity, rsp);
        return null;
    }


    @RequestMapping(value=LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String mergeSnapshot(HttpEntity<String> entity) {
        MergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MergeSnapshotCmd.class);
        config.mergeSnapshotCmds.add(cmd);
        MergeSnapshotRsp rsp = new MergeSnapshotRsp();
        Long size = config.snapshotToVolumeSize.get(cmd.getVolumeUuid());
        rsp.setSize(size == null ? 0 : size);
        Long asize = config.snapshotToVolumeActualSize.get(cmd.getVolumeUuid());
        rsp.setActualSize(asize == null ? 0 : asize);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_VOLUME_SIZE, method= RequestMethod.POST)
    public @ResponseBody
    String getVolumeActualSize(HttpEntity<String> entity) {
        GetVolumeSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeSizeCmd.class);
        GetVolumeSizeRsp rsp = new GetVolumeSizeRsp();

        config.getVolumeSizeCmds.add(cmd);
        Long asize = config.getVolumeSizeCmdActualSize.get(cmd.volumeUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        Long size = config.getVolumeSizeCmdSize.get(cmd.volumeUuid);
        rsp.size = size == null ? 0 : size;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.OFFLINE_MERGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String offlineMerge(HttpEntity<String> entity) {
        OfflineMergeSnapshotCmd cmd =  JSONObjectUtil.toObject(entity.getBody(), OfflineMergeSnapshotCmd.class);
        config.offlineMergeSnapshotCmds.add(cmd);
        OfflineMergeSnapshotRsp rsp = new OfflineMergeSnapshotRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.WRITE_VM_METADATA_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String writeVmMetadata(HttpEntity<String> entity) {
        WriteVmMetadataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), WriteVmMetadataCmd.class);
        config.writeVmMetadataCmds.add(cmd);
        reply(entity, new WriteVmMetadataRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_VM_INSTANCE_METADATA_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getVmInstanceMetadata(HttpEntity<String> entity) {
        GetVmInstanceMetadataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVmInstanceMetadataCmd.class);
        config.getVmInstanceMetadataCmds.add(cmd);
        GetVmInstanceMetadataRsp rsp = new GetVmInstanceMetadataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.SCAN_VM_METADATA_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String scanVmMetadata(HttpEntity<String> entity) {
        ScanVmMetadataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ScanVmMetadataCmd.class);
        config.scanVmMetadataCmds.add(cmd);
        ScanVmMetadataRsp rsp = new ScanVmMetadataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CLEANUP_VM_METADATA_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String cleanupVmMetadata(HttpEntity<String> entity) {
        CleanupVmMetadataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CleanupVmMetadataCmd.class);
        config.cleanupVmMetadataCmds.add(cmd);
        reply(entity, new CleanupVmMetadataRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.PREFIX_REBASE_BACKING_FILES_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String prefixRebaseBackingFiles(HttpEntity<String> entity) {
        PrefixRebaseBackingFilesCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PrefixRebaseBackingFilesCmd.class);
        PrefixRebaseBackingFilesRsp rsp = new PrefixRebaseBackingFilesRsp();
        rsp.rebasedCount = cmd.filePaths == null ? 0 : cmd.filePaths.size();
        reply(entity, rsp);
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllException(Exception ex) {
        logger.warn(ex.getMessage(), ex);
        ModelAndView model = new ModelAndView("error/generic_error");
        model.addObject("errMsg", ex.getMessage());
        return model;
    }
}
