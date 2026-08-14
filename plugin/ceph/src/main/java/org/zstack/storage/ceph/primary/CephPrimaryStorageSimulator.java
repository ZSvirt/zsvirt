package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.*;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingCmd;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingRsp;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageSimulator {
    CLogger logger = Utils.getLogger(CephPrimaryStorageSimulator.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private CephPrimaryStorageSimulatorConfig config;

    private Map<String, Long> bitSizeMap = new HashMap<String, Long>();

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

    private CephPrimaryStorageConfig getConfig(AgentCommand cmd) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(BackupStorageVO_.name);
        q.add(BackupStorageVO_.uuid, Op.EQ, cmd.getUuid());
        String name = q.findValue();

        CephPrimaryStorageConfig c = config.config.get(name);
        if (c == null) {
            throw new CloudRuntimeException(String.format("cannot find CephPrimaryStorageConfig by name[%s], uuid[%s]", name, cmd.getUuid()));
        }

        return c;
    }

    @RequestMapping(value= CephPrimaryStorageBase.GET_FACTS, method= RequestMethod.POST)
    public @ResponseBody
    String getFacts(HttpEntity<String> entity) {
        GetFactsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetFactsCmd.class);
        GetFactsRsp rsp = new GetFactsRsp();

        config.getFactsCmds.add(cmd);
        String fsid = config.getFactsCmdFsid.get(cmd.monUuid);
        if (fsid == null) {
            CephPrimaryStorageConfig c = getConfig(cmd);
            fsid = c.fsid;
        }

        rsp.fsid = fsid;
        rsp.monAddr = config.monAddr.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_POOL_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deletePool(HttpEntity<String> entity) {
        DeletePoolCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeletePoolCmd.class);
        config.deletePoolCmds.add(cmd);
        reply(entity, new DeletePoolRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String initialize(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        CephPrimaryStorageConfig cpc = getConfig(cmd);

        InitRsp rsp = new InitRsp();
        if (!config.monInitSuccess) {
            rsp.setError("on purpose");
        } else {
            rsp.fsid = cpc.fsid;
            rsp.userKey = Platform.getUuid();
            rsp.totalCapacity = cpc.totalCapacity;
            rsp.availableCapacity = cpc.availCapacity;
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CHECK_POOL_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkPool(HttpEntity<String> entity) {
        CheckCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckCmd.class);
        CephPrimaryStorageConfig cpc = getConfig(cmd);

        CheckRsp rsp = new CheckRsp();
        if (!config.monInitSuccess) {
            rsp.setError("on purpose");
        } else {
            rsp.setSuccess(true);
        }

        reply(entity, rsp);
        return null;
    }


    private void setCapacity(AgentCommand cmd, AgentResponse rsp, long size) {
        CephPrimaryStorageConfig cpc = getConfig(cmd);
        rsp.totalCapacity = cpc.totalCapacity;
        rsp.availableCapacity = cpc.availCapacity + size;
    }

    @RequestMapping(value= CephPrimaryStorageMonBase.PING_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String pingMon(HttpEntity<String> entity) {
        PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PingCmd.class);
        Boolean success = config.pingCmdSuccess.get(cmd.monUuid);
        PingRsp rsp = new PingRsp();
        rsp.setSuccess(success == null ? true : success);
        if (!rsp.isSuccess()) {
            rsp.setError("on purpose");
        }
        rsp.failure = config.pingCmdOperationFailure.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CREATE_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createEmptyVolume(HttpEntity<String> entity) throws InterruptedException {
        if (config.synchronizedCreateEmptyVolume) {
            synchronized (config) {
                doCreateEmptyVolume(entity);
            }
        } else {
            doCreateEmptyVolume(entity);
        }
        return null;
    }

    String doCreateEmptyVolume(HttpEntity<String> entity) throws InterruptedException {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        config.createEmptyVolumeCmds.add(cmd);

        CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        //setCapacity(cmd, rsp, -cmd.getSize());
        for (String path : bitSizeMap.keySet()) {
            if (!path.equals(cmd.getInstallPath())) {
                continue;
            }
            rsp.setError(String.format("File exists[%s]", cmd.getInstallPath()));
            reply(entity, rsp);
            return null;
        }
        TimeUnit.SECONDS.sleep(2);
        bitSizeMap.put(cmd.getInstallPath(), cmd.getSize());
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createKvmSecret(HttpEntity<String> entity) {
        CreateKvmSecretCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateKvmSecretCmd.class);
        config.createKvmSecretCmds.add(cmd);
        reply(entity, new KVMAgentCommands.AgentResponse());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String doDelete(HttpEntity<String> entity) {
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        config.deleteCmds.add(cmd);
        Long size = bitSizeMap.get(cmd.getInstallPath());
        size = size == null ? 0 : size;

        DeleteRsp rsp = new DeleteRsp();
        setCapacity(cmd, rsp, size);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createSnapshot(HttpEntity<String> entity) {
        CreateSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateSnapshotCmd.class);
        config.createSnapshotCmds.add(cmd);

        CreateSnapshotRsp rsp = new CreateSnapshotRsp();
        Long size = config.createSnapshotCmdSize.get(cmd.getVolumeUuid());
        rsp.setActualSize(size == null ? 0 : size);
        rsp.setInstallPath(cmd.getSnapshotPath());

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deleteSnapshot(HttpEntity<String> entity) {
        DeleteSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteSnapshotCmd.class);
        config.deleteSnapshotCmds.add(cmd);

        reply(entity, new DeleteSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String protectSnapshot(HttpEntity<String> entity) {
        ProtectSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ProtectSnapshotCmd.class);
        config.protectSnapshotCmds.add(cmd);

        reply(entity, new ProtectSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String unprotectSnapshot(HttpEntity<String> entity) {
        UnprotectedSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UnprotectedSnapshotCmd.class);
        config.unprotectedSnapshotCmds.add(cmd);

        reply(entity, new UnprotectedSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CLONE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String clone(HttpEntity<String> entity) {
        CloneCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CloneCmd.class);
        config.cloneCmds.add(cmd);

        CloneRsp rsp = new CloneRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.FLATTEN_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String flatten(HttpEntity<String> entity) {
        FlattenCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlattenCmd.class);
        config.flattenCmds.add(cmd);

        reply(entity, new FlattenRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CP_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String cp(HttpEntity<String> entity) {
        CpRsp rsp = new CpRsp();
        CpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CpCmd.class);
        config.cpCmds.add(cmd);

        Long size = config.cpCmdSize.get(cmd.resourceUuid);
        rsp.size = size == null ? 0 : size;
        Long asize = config.cpCmdActualSize.get(cmd.resourceUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        rsp.installPath = cmd.dstPath;

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getVolumeSize(HttpEntity<String> entity) {
        GetVolumeSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetVolumeSizeCmd.class);
        config.getVolumeSizeCmds.add(cmd);

        Long asize = config.getVolumeSizeCmdActualSize.get(cmd.volumeUuid);
        GetVolumeSizeRsp rsp = new GetVolumeSizeRsp();
        rsp.actualSize = asize == null ? 0 : asize;
        Long size = config.getVolumeSizeCmdSize.get(cmd.volumeUuid);
        rsp.size = size == null ? 0 : size;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.SFTP_UPLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String sftpUpload(HttpEntity<String> entity) {
        SftpUpLoadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpUpLoadCmd.class);
        config.sftpUpLoadCmds.add(cmd);

        reply(entity, new SftpUpLoadCmd());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.SFTP_DOWNLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String sftpDownload(HttpEntity<String> entity) {
        SftpDownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpDownloadCmd.class);
        config.sftpDownloadCmds.add(cmd);

        reply(entity, new SftpDownloadRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String rollback(HttpEntity<String> entity) {
        RollbackSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RollbackSnapshotCmd.class);
        config.rollbackSnapshotCmds.add(cmd);

        reply(entity, new RollbackSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.KVM_HA_SETUP_SELF_FENCER, method= RequestMethod.POST)
    public @ResponseBody
    String setupSelfFencer(HttpEntity<String> entity) {
        KvmSetupSelfFencerCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KvmSetupSelfFencerCmd.class);
        config.kvmSetupSelfFencerCmds.add(cmd);
        reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.KVM_HA_CANCEL_SELF_FENCER, method= RequestMethod.POST)
    public @ResponseBody
    String cancelSelfFencer(HttpEntity<String> entity) {
        KvmCancelSelfFencerCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KvmCancelSelfFencerCmd.class);
        config.kvmCancelSelfFencerCmds.add(cmd);
        reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_IMAGE_CACHE, method= RequestMethod.POST)
    public @ResponseBody
    String deleteImageCache(HttpEntity<String> entity) {
        DeleteImageCacheCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteImageCacheCmd.class);
        config.deleteImageCacheCmds.add(cmd);
        reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CHECK_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkBitsPath(HttpEntity<String> entity) {
        CheckIsBitsExistingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckIsBitsExistingCmd.class);
        config.checkBitsPathCmds.add(cmd);

        CheckIsBitsExistingRsp rsp = new CheckIsBitsExistingRsp();
        rsp.setExisting(true);

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getDownlaodProgress(HttpEntity<String> entity) {
        GetDownloadBitsFromKVMHostProgressCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetDownloadBitsFromKVMHostProgressCmd.class);
        config.getDownLoadProgressCmds.add(cmd);

        GetDownloadBitsFromKVMHostProgressRsp rsp = new GetDownloadBitsFromKVMHostProgressRsp();
        rsp.setTotalSize(1L);

        reply(entity, rsp);
        return null;
    }
}
