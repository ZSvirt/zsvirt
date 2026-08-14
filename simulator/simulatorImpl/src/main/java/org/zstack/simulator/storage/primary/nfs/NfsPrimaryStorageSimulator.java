package org.zstack.simulator.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.*;
import org.zstack.storage.primary.nfs.NfsPrimaryToSftpBackupKVMBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class NfsPrimaryStorageSimulator {
    CLogger logger = Utils.getLogger(NfsPrimaryStorageSimulator.class);
    
    @Autowired
    private RESTFacade restf;
    @Autowired
    private NfsPrimaryStorageSimulatorConfig config;
    @Autowired
    private VolumeSnapshotKvmSimulator volumeSnapshotKvmSimulator;

    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    class Capacity {
        long total;
        long avail;
    }

    private void reply(HttpEntity<String> entity, NfsPrimaryStorageAgentResponse rsp) {
        replyer.reply(entity, rsp);
    }

    private Map<String, Capacity> capacityMap = new HashMap<String, Capacity>();

    private void setCapacity(NfsPrimaryStorageAgentCommand cmd, NfsPrimaryStorageAgentResponse rsp) {
        rsp.setTotalCapacity(config.totalCapacity);
        rsp.setAvailableCapacity(config.availableCapacity);
    }

    @AsyncThread
    private void doGetCapacity(HttpEntity<String> entity) {
        GetCapacityCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetCapacityCmd.class);
        GetCapacityResponse rsp = new GetCapacityResponse();
        setCapacity(cmd, rsp);
        reply(entity, rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.GET_VOLUME_BASE_IMAGE_PATH, method=RequestMethod.POST)
    private @ResponseBody String getVolumeBaseImagePath(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        GetVolumeBaseImagePathCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeBaseImagePathCmd.class);
        GetVolumeBaseImagePathRsp rsp = new GetVolumeBaseImagePathRsp();
        rsp.path = config.getVolumeBaseImagePaths.get(cmd.volumeUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.UNMOUNT_PRIMARY_STORAGE_PATH, method=RequestMethod.POST)
    public @ResponseBody String nfsUnmount(@RequestBody String body) {
        if (config.unmountException) {
            throw new CloudRuntimeException("unmount exception on purpose");
        }

        UnmountCmd cmd = JSONObjectUtil.toObject(body, UnmountCmd.class);
        AgentResponse rsp = new AgentResponse();
        if (config.unmountSuccess) {
            rsp.setSuccess(true);
            config.unmountCmds.add(cmd);
            logger.debug(String.format("Unmount %s", cmd.getMountPath()));
        } else {
            rsp.setSuccess(false);
            rsp.setError("Fail umount on purpose");
        }
        return JSONObjectUtil.toJsonString(rsp);
    }

    @AsyncThread
    private void doCreateRootVolumeFromTemplate(HttpEntity<String> entity) throws InterruptedException {
        CreateRootVolumeFromTemplateCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateRootVolumeFromTemplateCmd.class);
        CreateRootVolumeFromTemplateResponse rsp = new CreateRootVolumeFromTemplateResponse();
        if (!config.createRootVolumeFromTemplateSuccess) {
            rsp.setSuccess(false);
            rsp.setError("Fail create on purpose");
        } else {
            logger.debug(String.format("Created root volume from cached template %s to %s", cmd.getTemplatePathInCache(), cmd.getInstallUrl()));
        }
        
        reply(entity, rsp);
    }


    @RequestMapping(value=NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH, method=RequestMethod.POST)
    public @ResponseBody String nfsMount(@RequestBody String body) {
        if (config.mountException) {
            throw new CloudRuntimeException("mount exception on purpose");
        }

        MountCmd cmd = JSONObjectUtil.toObject(body, MountCmd.class);
        MountAgentResponse rsp = new MountAgentResponse();
        if (config.mountSuccess) {
            Capacity cap = new Capacity();
            cap.total = config.totalCapacity;
            cap.avail = config.availableCapacity;
            capacityMap.put(cmd.getUuid(), cap);

            rsp.setTotalCapacity(cap.total);
            rsp.setAvailableCapacity(cap.avail);

            config.mountCmds.add(cmd);

            logger.debug(String.format("mount %s to %s", cmd.getUrl(), cmd.getMountPath()));
        } else {
            rsp.setSuccess(false);
            rsp.setError("Fail mount on purpose");
        }
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.SYNC_GET_CAPACITY_PATH, method=RequestMethod.POST)
    private @ResponseBody String syncGetCapacity(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        GetCapacityCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetCapacityCmd.class);
        GetCapacityResponse rsp = new GetCapacityResponse();
        setCapacity(cmd, rsp);
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.GET_CAPACITY_PATH, method=RequestMethod.POST)
    private @ResponseBody String getCapacity(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doGetCapacity(entity);
        return null;
    }
    
    @RequestMapping(value=NfsPrimaryStorageKVMBackend.CREATE_VOLUME_FROM_TEMPLATE_PATH, method=RequestMethod.POST)
    private @ResponseBody String createRootVolumeFromTemplate(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        if (config.createRootVolumeFromTemplateException) {
            throw new CloudRuntimeException("Fail download on purpose");
        } else {
            doCreateRootVolumeFromTemplate(entity);
        }
        return null;
    }

    @RequestMapping(value=NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH, method=RequestMethod.POST)
    private @ResponseBody String downloadFromSftp(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        downloadFromSftp(entity);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.PING_PATH, method=RequestMethod.POST)
    private @ResponseBody String ping(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        NfsPrimaryStorageAgentResponse rsp = new NfsPrimaryStorageAgentResponse();
        if (!config.pingSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.pingCmds.add(JSONObjectUtil.toObject(entity.getBody(), PingCmd.class));
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.DELETE_PATH, method=RequestMethod.POST)
    private @ResponseBody String delete(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        DeleteResponse rsp = new DeleteResponse();
        if (!config.deleteSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            // we don't know if this is deleting snapshot
            // volumeSnapshotKvmSimulator will ignore the call if it's not
            volumeSnapshotKvmSimulator.delete(cmd.getInstallPath());
            config.deleteCmds.add(cmd);
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.MOVE_BITS_PATH, method=RequestMethod.POST)
    private @ResponseBody String move(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        move(entity);
        return null;
    }

    @AsyncThread
    private void move(HttpEntity<String> entity) {
        MoveBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MoveBitsCmd.class);
        MoveBitsRsp rsp = new MoveBitsRsp();
        if (!config.moveBitsSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.moveBitsCmds.add(cmd);
        }

        reply(entity, rsp);
    }

    @AsyncThread
    private void downloadFromSftp(HttpEntity<String> entity) {
        DownloadBitsFromSftpBackupStorageCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadBitsFromSftpBackupStorageCmd.class);
        DownloadBitsFromSftpBackupStorageResponse rsp = new DownloadBitsFromSftpBackupStorageResponse();
        if (!config.downloadFromSftpSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(entity.getBody());
            config.downloadFromSftpCmds.add(cmd);
            config.imageCache.add(cmd.getPrimaryStorageInstallPath());
        }

        reply(entity, rsp);
    }


    @RequestMapping(value=NfsPrimaryToSftpBackupKVMBackend.UPLOAD_TO_SFTP_PATH, method=RequestMethod.POST)
    private @ResponseBody String copyToSftp(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        copyToSftp(entity);
        return null;
    }

    @AsyncThread
    private void copyToSftp(HttpEntity<String> entity) {
        UploadToSftpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UploadToSftpCmd.class);
        UploadToSftpResponse rsp = new UploadToSftpResponse();
        if (!config.uploadToSftp || cmd.getPrimaryStorageInstallPath().equals(config.backupSnapshotFailurePrimaryStorageInstallPath)) {
            rsp.setError("no purpose");
            rsp.setSuccess(false);
        } else {
            config.uploadToSftpCmds.add(cmd);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE, method=RequestMethod.POST)
    private @ResponseBody String offlineMergeSnapshot(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        OfflineMergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), OfflineMergeSnapshotCmd.class);
        OfflineMergeSnapshotRsp rsp = new OfflineMergeSnapshotRsp();
        if (!config.offlineMergeSnapshotSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            volumeSnapshotKvmSimulator.merge(cmd.getSrcPath(), cmd.getDestPath(), cmd.isFullRebase());
            config.offlineMergeSnapshotCmds.add(cmd);
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.CHECK_BITS_PATH, method=RequestMethod.POST)
    private @ResponseBody String checkBits(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        checkBits(entity);
        return null;
    }

    @AsyncThread
    private void checkBits(HttpEntity<String> entity) {
        CheckIsBitsExistingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckIsBitsExistingCmd.class);
        CheckIsBitsExistingRsp rsp = new CheckIsBitsExistingRsp();
        if (config.checkImageSuccess) {
            rsp.setExisting(config.imageCache.contains(cmd.getInstallPath()));
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH, method=RequestMethod.POST)
    private @ResponseBody String createEmptyVolume(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doCreateEmptyVolume(entity);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH, method=RequestMethod.POST)
    private @ResponseBody String createTemplateFromRootVolume(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doCreateTemplateFromRootVolume(entity);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH, method=RequestMethod.POST)
    private @ResponseBody String revertVolumeFromSnapshot(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        RevertVolumeFromSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RevertVolumeFromSnapshotCmd.class);
        RevertVolumeFromSnapshotResponse rsp = new RevertVolumeFromSnapshotResponse();
        if (config.revertVolumeFromSnapshotSuccess) {
            config.revertVolumeFromSnapshotCmds.add(cmd);
            rsp = volumeSnapshotKvmSimulator.revert(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.REBASE_MERGE_SNAPSHOT_PATH, method=RequestMethod.POST)
    private @ResponseBody String rebaseAndMergeSnapshot(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        rebaseAndMergeSnapshot(entity);
        return null;
    }

    @AsyncThread
    private void rebaseAndMergeSnapshot(HttpEntity<String> entity) {
        RebaseAndMergeSnapshotsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RebaseAndMergeSnapshotsCmd.class);
        RebaseAndMergeSnapshotsResponse rsp = new RebaseAndMergeSnapshotsResponse();
        if (!config.rebaseAndMergeSnapshotSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            Long size = config.rebaseAndMergeSnapshotsCmdSize.get(cmd.getVolumeUuid());
            rsp.setSize(size == null ? 0 : size);
            Long aszie = config.rebaseAndMergeSnapshotsCmdActualSize.get(cmd.getVolumeUuid());
            rsp.setActualSize(aszie == null ? 0 : aszie);
            config.rebaseAndMergeSnapshotsCmds.add(cmd);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH, method=RequestMethod.POST)
    private @ResponseBody String getVolumeSize(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        GetVolumeActualSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeActualSizeCmd.class);
        config.getVolumeSizeCmds.add(cmd);

        GetVolumeActualSizeRsp rsp = new GetVolumeActualSizeRsp();
        Long asize = config.getVolumeSizeCmdActualSize.get(cmd.volumeUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        Long size = config.getVolumeSizeCmdSize.get(cmd.volumeUuid);
        rsp.size = size == null ? 0 : size;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.MERGE_SNAPSHOT_PATH, method=RequestMethod.POST)
    private @ResponseBody String mergeSnapshot(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        mergeSnapshot(entity);
        return null;
    }

    @AsyncThread
    private void mergeSnapshot(HttpEntity<String> entity) {
        MergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MergeSnapshotCmd.class);
        MergeSnapshotResponse rsp = new MergeSnapshotResponse();
        if (!config.mergeSnapshotSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            Long size = config.mergeSnapshotCmdSize.get(cmd.getVolumeUuid());
            rsp.setSize(size == null ? 0 : size);
            Long asize = config.mergeSnapshotCmdActualSize.get(cmd.getVolumeUuid());
            rsp.setActualSize(asize == null ? 0 : asize);
            config.mergeSnapshotCmds.add(cmd);
        }
        reply(entity, rsp);
    }

    @AsyncThread
    private void doCreateTemplateFromRootVolume(HttpEntity<String> entity) {
        CreateTemplateFromVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateTemplateFromVolumeCmd.class);
        CreateTemplateFromVolumeRsp rsp = new CreateTemplateFromVolumeRsp();
        if (!config.createTemplateFromRootVolumeSuccess) {
            rsp.setSuccess(false);
            rsp.setError("Fail create template on purpose");
        } else {
            logger.debug(String.format("successfully create template from volume, %s", entity.getBody()));
        }
        reply(entity, rsp);
    }

    @AsyncThread
    private void doCreateEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        CreateEmptyVolumeResponse rsp = new CreateEmptyVolumeResponse();
        if (!config.createEmptyVolumeSuccess) {
            rsp.setError("failed on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(String.format("create empty volume[uuid:%s,  mountPath:%s, size:%s]", cmd.getUuid(), cmd.getInstallUrl(), cmd.getSize()));
        }
        reply(entity, rsp);
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.REMOUNT_PATH, method=RequestMethod.POST)
    private @ResponseBody String remount(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        RemountCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RemountCmd.class);
        NfsPrimaryStorageAgentResponse rsp = new NfsPrimaryStorageAgentResponse();
        if (!config.remountSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            rsp.setTotalCapacity(config.totalCapacity);
            rsp.setAvailableCapacity(config.availableCapacity);
            config.remountCmds.add(cmd);
        }
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH, method=RequestMethod.POST)
    private @ResponseBody String updateMountPoint(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        UpdateMountPointCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UpdateMountPointCmd.class);
        UpdateMountPointRsp rsp = new UpdateMountPointRsp();
        config.updateMountPointCmds.add(cmd);
        rsp.setTotalCapacity(config.totalCapacity);
        rsp.setAvailableCapacity(config.availableCapacity);
        reply(entity, rsp);
        return null;
    }
}
