package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.primary.smp.KvmBackend.*;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.storage.primary.smp.SftpBackupStorageKvmDownloader.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH;
import static org.zstack.storage.primary.smp.SftpBackupStorageKvmUploader.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH;

/**
 * Created by xing5 on 2016/3/27.
 */
public class SMPPrimaryStorageSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private SMPPrimaryStorageSimulatorConfig config;

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

    @RequestMapping(value=KvmBackend.CONNECT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String connect(HttpEntity<String> entity) {
        ConnectCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConnectCmd.class);
        config.connectCmds.add(cmd);
        ConnectRsp rsp = new ConnectRsp();
        rsp.totalCapacity = config.totalCapacity;
        rsp.availableCapacity = config.availableCapcacity;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KvmBackend.CREATE_VOLUME_FROM_CACHE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createRootVolume(HttpEntity<String> entity) {
        CreateVolumeFromCacheCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVolumeFromCacheCmd.class);
        config.createVolumeFromCacheCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=KvmBackend.DELETE_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deleteBits(HttpEntity<String> entity) {
        DeleteBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteBitsCmd.class);
        config.deleteBitsCmds.add(cmd);
        reply(entity, new DeleteRsp());
        return null;
    }

    @RequestMapping(value=KvmBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createTemplateFromVolume(HttpEntity<String> entity) {
        CreateTemplateFromVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateTemplateFromVolumeCmd.class);
        config.createTemplateFromVolumeCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String uploadBitsToSftp(HttpEntity<String> entity) {
        SftpUploadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpUploadBitsCmd.class);
        config.uploadBitsCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String downloadBitsFromSftp(HttpEntity<String> entity) {
        SftpDownloadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpDownloadBitsCmd.class);
        config.downloadBitsCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String revertVolume(HttpEntity<String> entity) {
        RevertVolumeFromSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RevertVolumeFromSnapshotCmd.class);
        config.revertVolumeFromSnapshotCmds.add(cmd);
        RevertVolumeFromSnapshotRsp rsp = new RevertVolumeFromSnapshotRsp();
        rsp.newVolumeInstallPath = "new_path";
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KvmBackend.MERGE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String mergeSnapshot(HttpEntity<String> entity) {
        MergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MergeSnapshotCmd.class);
        config.mergeSnapshotCmds.add(cmd);
        MergeSnapshotRsp rsp = new MergeSnapshotRsp();

        Long size = config.mergeSnapshotCmdSize.get(cmd.volumeUuid);
        rsp.size = size == null ? 0 : size;
        Long asize = config.mergeSnapshotCmdActualSize.get(cmd.volumeUuid);
        rsp.actualSize = asize == null ? 0 : asize;

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KvmBackend.GET_VOLUME_SIZE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getVolumeSize(HttpEntity<String> entity) {
        GetVolumeSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeSizeCmd.class);
        config.getVolumeSizeCmds.add(cmd);

        GetVolumeSizeRsp rsp = new GetVolumeSizeRsp();
        Long asize = config.getVolumeSizeCmdActualSize.get(cmd.volumeUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        Long size = config.getVolumeSizeCmdSize.get(cmd.volumeUuid);
        rsp.size = size == null ? 0 : size;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KvmBackend.OFFLINE_MERGE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String offlineMerge(HttpEntity<String> entity) {
        OfflineMergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), OfflineMergeSnapshotCmd.class);
        config.offlineMergeSnapshotCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=KvmBackend.CREATE_EMPTY_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        config.createEmptyVolumeCmds.add(cmd);
        reply(entity, new AgentRsp());
        return null;
    }

    @RequestMapping(value=KvmBackend.CHECK_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkBits(HttpEntity<String> entity) {
        CheckBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckBitsCmd.class);
        config.checkBitsCmds.add(cmd);
        CheckBitsRsp rsp = new CheckBitsRsp();
        rsp.existing = true;
        reply(entity, rsp);
        return null;
    }
}
