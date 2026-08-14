package org.zstack.storage.ceph.backup;

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
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.*;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig.CephBackupStorageConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 7/28/2015.
 */
public class CephBackupStorageSimulator {
    CLogger logger = Utils.getLogger(CephBackupStorageSimulator.class);

    @Autowired
    private CephBackupStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

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

    private CephBackupStorageConfig getConfig(AgentCommand cmd) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.name);
        q.add(BackupStorageVO_.uuid, Op.EQ, cmd.getUuid());
        String name = q.findValue();

        CephBackupStorageConfig c = config.config.get(name);
        if (c == null) {
            throw new CloudRuntimeException(String.format("cannot find CephBackupStorageConfig by name[%s], uuid[%s]", name, cmd.getUuid()));
        }

        c.name = name;

        return c;
    }

    @RequestMapping(value= CephBackupStorageBase.GET_FACTS, method= RequestMethod.POST)
    public @ResponseBody
    String getFacts(HttpEntity<String> entity) {
        GetFactsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetFactsCmd.class);
        GetFactsRsp rsp = new GetFactsRsp();

        config.getFactsCmds.add(cmd);
        String fsid = config.getFactsCmdFsid.get(cmd.monUuid);
        if (fsid == null) {
            CephBackupStorageConfig c = getConfig(cmd);
            fsid = c.fsid;
        }

        rsp.fsid = fsid;
        rsp.monAddr = config.monAddr.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephBackupStorageMonBase.PING_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String pingMon(HttpEntity<String> entity) {
        CephBackupStorageMonBase.PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CephBackupStorageMonBase.PingCmd.class);
        Boolean success = config.pingCmdSuccess.get(cmd.monUuid);
        CephBackupStorageMonBase.PingRsp rsp = new CephBackupStorageMonBase.PingRsp();
        rsp.setSuccess(success == null ? true : success);
        if (!rsp.isSuccess()) {
            rsp.setError("on purpose");
        }
        rsp.failure = config.pingCmdOperationFailure.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.GET_IMAGE_SIZE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getImageSize(HttpEntity<String> entity) {
        GetImageSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetImageSizeCmd.class);
        config.getImageSizeCmds.add(cmd);

        GetImageSizeRsp rsp = new GetImageSizeRsp();
        Long size = config.getImageSizeCmdSize.get(cmd.imageUuid);
        rsp.size = size == null ? 0 : size;
        Long asize = config.getImageSizeCmdActualSize.get(cmd.imageUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String initialize(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        CephBackupStorageConfig cbc = getConfig(cmd);
        config.initCmds.add(cmd);

        DebugUtils.Assert(cbc.fsid != null, String.format("fsid for ceph backup storage[%s] is null", cbc.name));

        InitRsp rsp = new InitRsp();

        if (!config.monInitSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            rsp.fsid = cbc.fsid;
            rsp.totalCapacity = cbc.totalCapacity;
            rsp.availableCapacity = cbc.availCapacity;
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephBackupStorageBase.CHECK_POOL_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String checkPool(HttpEntity<String> entity) {
        CheckCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckCmd.class);
        CephBackupStorageConfig cbc = getConfig(cmd);

        CheckRsp rsp = new CheckRsp();
        if (!config.monInitSuccess) {
            rsp.setError("on purpose");
        } else {
            rsp.setSuccess(true);
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DOWNLOAD_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String download(HttpEntity<String> entity) {
        DownloadRsp rsp = new DownloadRsp();
        DownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadCmd.class);
        config.downloadCmds.add(cmd);

        Long size = config.imageSize.get(cmd.imageUuid);
        rsp.setSize(size == null ? 0 : size);
        Long asize = config.imageActualSize.get(cmd.imageUuid);
        rsp.setActualSize(asize == null ? 0 : asize);

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DELETE_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String doDelete(HttpEntity<String> entity) {
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        config.deleteCmds.add(cmd);
        DeleteRsp rsp = new DeleteRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.CHECK_IMAGE_METADATA_FILE_EXIST, method= RequestMethod.POST)
    public @ResponseBody
    String checkMetaDataFile(HttpEntity<String> entity) {
        CheckImageMetaDataFileExistCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckImageMetaDataFileExistCmd.class);
        config.checkMetadataFileCmds.add(cmd);
        CheckImageMetaDataFileExistRsp rsp = new CheckImageMetaDataFileExistRsp();
        rsp.setExist(true);
        rsp.setBackupStorageMetaFileName("bs_ceph_info.json");
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DELETE_IMAGES_METADATA, method= RequestMethod.POST)
    public @ResponseBody
    String deleteImagesMetadata(HttpEntity<String> entity) {
        DeleteImageInfoFromMetaDataFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteImageInfoFromMetaDataFileCmd.class);
        config.deleteImageInfoFromMetadataFileCmds.add(cmd);
        DeleteImageInfoFromMetaDataFileRsp rsp = new DeleteImageInfoFromMetaDataFileRsp();
        rsp.setRet(0);
        rsp.setOut("success delete");
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DUMP_IMAGE_METADATA_TO_FILE, method= RequestMethod.POST)
    public @ResponseBody
    String dumpImagesMetadataToFile(HttpEntity<String> entity) {
        DumpImageInfoToMetaDataFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DumpImageInfoToMetaDataFileCmd.class);
        config.dumpImageInfoToMetaDataFileCmds.add(cmd);
        DumpImageInfoToMetaDataFileRsp rsp = new DumpImageInfoToMetaDataFileRsp();
        rsp.setSuccess(true);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.GET_IMAGES_METADATA, method= RequestMethod.POST)
    public @ResponseBody
    String getImagesMetadataToFile(HttpEntity<String> entity) {
        GetImagesMetaDataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetImagesMetaDataCmd.class);
        config.getImageInfoToMetaDataFileCmds.add(cmd);
        GetImagesMetaDataRsp rsp = new GetImagesMetaDataRsp();
        rsp.setSuccess(true);
        rsp.setImagesMetadata("{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}");
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.GET_LOCAL_FILE_SIZE, method= RequestMethod.POST)
    public @ResponseBody
    String getLocalFileSize(HttpEntity<String> entity) {
        GetLocalFileSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetLocalFileSizeCmd.class);
        GetLocalFileSizeRsp rsp = new GetLocalFileSizeRsp();
        rsp.size = 0;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.FILE_DOWNLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String fileDownload(HttpEntity<String> entity) {
        DownloadFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadFileCmd.class);
        DownloadFileResponse rsp = new DownloadFileResponse();
        rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e";
        rsp.size = 0;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.FILE_UPLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String fileUpload(HttpEntity<String> entity) {
        UploadFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UploadFileCmd.class);
        UploadFileResponse rsp = new UploadFileResponse();
        rsp.directUploadUrl = "http://127.0.0.1:7761/ceph/file/direct/upload";
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.FILE_DOWNLOAD_PROGRESS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String fileDownloadProgress(HttpEntity<String> entity) {
        GetDownloadFileProgressCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetDownloadFileProgressCmd.class);
        GetDownloadFileProgressResponse rsp = new GetDownloadFileProgressResponse();
        rsp.completed = true;
        rsp.progress = 100;
        rsp.size = 0;
        rsp.actualSize = 0;
        rsp.installPath = "/tmp/test-software-package/unzipInstallPath";
        rsp.format = "qcow2";
        rsp.lastOpTime = System.currentTimeMillis();
        rsp.downloadSize = 0;
        rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e";
        rsp.supportSuspend = true;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DELETE_FILES_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deleteFiles(HttpEntity<String> entity) {
        DeleteFilesCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteFilesCmd.class);
        DeleteFilesResponse rsp = new DeleteFilesResponse();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.UNZIP_FILE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String unzipFile(HttpEntity<String> entity) {
        UnzipFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UnzipFileCmd.class);
        UnzipFileResponse rsp = new UnzipFileResponse();
        rsp.unzipInstallPath = "/tmp/test-software-package/unzipInstallPath";
        rsp.fileSizes = new java.util.HashMap<>();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String softwareUpgradePackageDeploy(HttpEntity<String> entity) {
        SoftwareUpgradePackageCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SoftwareUpgradePackageCmd.class);
        SoftwareUpgradePackageResponse rsp = new SoftwareUpgradePackageResponse();
        reply(entity, rsp);
        return null;
    }
}
