package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * Created by miao on 16-7-13.
 */
public class ImageStoreBackupStorageSimulator {
    CLogger logger = Utils.getLogger(ImageStoreBackupStorageSimulator.class);

    @Autowired
    private ImageStoreBackupStorageSimulatorConfig config;
    @Autowired
    private RESTFacade restf;

    private AsyncRESTReplyer replyer;

    @RequestMapping(value = ImageStoreBackupStorageConstant.CONNECT_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String connect(@RequestBody String body) {
        ImageStoreBackupStorageCommands.ConnectCmd cmd =
                JSONObjectUtil.toObject(body, ImageStoreBackupStorageCommands.ConnectCmd.class);
        ImageStoreBackupStorageCommands.ConnectResponse rsp = new ImageStoreBackupStorageCommands.ConnectResponse();
        if (!config.connectSuccess) {
            config.bsUuid = cmd.getUuid();
            rsp.setSuccess(false);
            rsp.setError("Fail connect on purpose");
        } else {
            rsp.setTotalSize(config.totalCapacity);
            rsp.setFreeSize(config.availableCapacity);
            logger.debug(String.format("Connect to ImageStore:UUID:[%s], %s", cmd.getUuid(), JSONObjectUtil.toJsonString(rsp)));
        }
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.ECHO_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String echo(@RequestBody String body) {
        return "";
    }

    private void reply(HttpEntity<String> entity, ImageStoreBackupStorageCommands.ImageStoreResponse rsp) {
        if (replyer == null) {
            replyer = new AsyncRESTReplyer();
        }
        replyer.reply(entity, rsp);
    }

    @AsyncThread
    private void doDownload(HttpEntity<String> entity) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
        ImageStoreBackupStorageCommands.DownloadImgCmd cmd = JSONObjectUtil.toObject(entity.getBody(),
                ImageStoreBackupStorageCommands.DownloadImgCmd.class);
        ImageStoreBackupStorageCommands.DownloadImgResponse rsp = new ImageStoreBackupStorageCommands.DownloadImgResponse();
        if (!config.downloadSuccess2) {
            rsp.setSuccess(false);
            rsp.setError("Fail download on purpose");
        } else {
            Long size = config.imageSizes.get(cmd.uuid);
            Long asize = config.imageActualSizes.get(cmd.uuid);
            rsp.setVirtualsize(size == null ? 0 : size);
            rsp.setDiskSize(asize == null ? 0 : asize);
            rsp.setBlobsum(config.imageMd5sum);
            rsp.setTotalSize(config.totalCapacity);
            rsp.setName("test-image");
            rsp.setId("46c25a0d3573a78505dceb9070a62503d869b667");
            long usedSize = 0;
            for (Long s : config.imageSizes.values()) {
                usedSize += s;
            }
            rsp.setFreeSize(config.totalCapacity - usedSize);
            logger.debug(String.format("Download %s", cmd.imgurl));
        }

        reply(entity, rsp);
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.DOWNLOAD_IMAGE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String download(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        if (!config.downloadSuccess1) {
            throw new CloudRuntimeException("Fail download on purpose");
        } else {
            doDownload(entity);
        }
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.EXPORT_IMAGE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String nfsCommitImage(HttpEntity<String> entity) {
        ImageStoreBackupStorageCommands.ExportImageCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), ImageStoreBackupStorageCommands.ExportImageCmd.class);

        ImageStoreBackupStorageCommands.ExportImageResponse rsp =
                new ImageStoreBackupStorageCommands.ExportImageResponse();

        rsp.setSuccess(true);
        rsp.setImgUrl("http://localhost/test-image/deafbeef");
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.DELEXP_IMAGE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String nfsDelExpImage(HttpEntity<String> entity) {
        ImageStoreBackupStorageCommands.DelExpImageCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), ImageStoreBackupStorageCommands.DelExpImageCmd.class);

        ImageStoreBackupStorageCommands.DelExpImageResponse rsp =
                new ImageStoreBackupStorageCommands.DelExpImageResponse();

        rsp.setSuccess(true);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.GET_IMAGE_INFO, method = RequestMethod.POST)
    public
    @ResponseBody
    String getImageActualSize(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        ImageStoreBackupStorageCommands.GetImageInfoCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), ImageStoreBackupStorageCommands.GetImageInfoCmd.class);
        ImageStoreBackupStorageCommands.ImageInfoResponse rsp = new ImageStoreBackupStorageCommands.ImageInfoResponse();
        Long asize = config.getImageSizeCmdActualSize.get(cmd.imageUuid);
        rsp.size = asize == null ? 0 : asize;
        Long size = config.getImageSizeCmdSize.get(cmd.imageUuid);
        rsp.virtualsize = size == null ? 0 : size;
        reply(entity, rsp);
        return null;
    }

    @AsyncThread
    private void doDelete(HttpEntity<String> entity) {
        ImageStoreBackupStorageCommands.ImageStoreResponse rsp = null;
        ImageStoreBackupStorageCommands.DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(),
                ImageStoreBackupStorageCommands.DeleteCmd.class);
        if (!config.deleteSuccess) {
            rsp = new ImageStoreBackupStorageCommands.ImageStoreResponse();
            rsp.setError("Fail delete on purpose");
            rsp.setSuccess(false);
        } else {
            config.deleteCmds.add(cmd);
            logger.debug(String.format("Deleted %s", cmd.getInstallPath()));
            rsp = (ImageStoreBackupStorageCommands.ImageStoreResponse) (new ImageStoreBackupStorageCommands.DeleteResponse());
        }

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

    @RequestMapping(value = ImageStoreBackupStorageConstant.DELETE_IMAGE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String delete(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doDelete(entity);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.PING_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String ping(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        if (config.pingException) {
            throw new CloudRuntimeException("on purpose");
        }
        doPing(entity);
        return null;
    }

    @AsyncThread
    private void doPing(HttpEntity<String> entity) {
        ImageStoreBackupStorageCommands.PingResponse rsp = new ImageStoreBackupStorageCommands.PingResponse();
        if (!config.pingSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            rsp.setUuid(config.bsUuid);
        }
        reply(entity, rsp);
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE, method = RequestMethod.POST)
    public
    @ResponseBody
    String generateMetadataFile(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        GenerateImageMetaDataFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GenerateImageMetaDataFileCmd.class);
        GenerateImageMetaDataFileRsp rsp = new GenerateImageMetaDataFileRsp();
        rsp.setBackupStorageMetaFileName("bs_file_info.json");
        rsp.setSuccess(true);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE, method = RequestMethod.POST)
    public
    @ResponseBody
    String dumpImagesInfoToMetadataFile(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DumpImageInfoToMetaDataFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DumpImageInfoToMetaDataFileCmd.class);
        DumpImageInfoToMetaDataFileRsp rsp = new DumpImageInfoToMetaDataFileRsp();
        rsp.setSuccess(true);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.DELETE_IMAGES_METADATA, method = RequestMethod.POST)
    public
    @ResponseBody
    String deleteImagesInfoFromMetadataFile(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteImageInfoFromMetaDataFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteImageInfoFromMetaDataFileCmd.class);
        DeleteImageInfoFromMetaDataFileRsp rsp = new DeleteImageInfoFromMetaDataFileRsp();
        rsp.setSuccess(true);
        rsp.setOut("delete success");
        rsp.setRet(0);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.ALLOCATE_UPLOAD_DIR, method = RequestMethod.POST)
    public
    @ResponseBody
    String allocateUploadSpace(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        AllocateUploadSpaceCmd cmd = JSONObjectUtil.toObject(entity.getBody(), AllocateUploadSpaceCmd.class);
        AllocateUploadSpaceResponse rsp = new AllocateUploadSpaceResponse();
        rsp.setSuccess(true);
        rsp.setUploadDir("/upload/dir");
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.GET_IMAGES_METADATA, method = RequestMethod.POST)
    public
    @ResponseBody
    String getImagesInfoFromMetadataFile(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        GetImagesMetaDataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetImagesMetaDataCmd.class);
        GetImagesMetaDataRsp rsp = new GetImagesMetaDataRsp();
        rsp.setSuccess(true);
        rsp.setImagesMetaData("{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}");
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String fileDownload(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DownloadFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadFileCmd.class);
        DownloadFileResponse rsp = new DownloadFileResponse();
        rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e";
        rsp.size = 0;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.FILE_UPLOAD_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String fileUpload(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        UploadFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UploadFileCmd.class);
        UploadFileResponse rsp = new UploadFileResponse();
        rsp.directUploadUrl = "http://127.0.0.1:7761/imagestore/file/direct/upload";
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PROGRESS_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String fileDownloadProgress(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
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
        rsp.supportSuspend = false;
        // syncJsonPost: return JSON in HTTP body (not async callback)
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.DELETE_FILES_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String deleteFiles(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteFilesCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteFilesCmd.class);
        DeleteFilesResponse rsp = new DeleteFilesResponse();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.UNZIP_FILE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String unzipFile(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        UnzipFileCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UnzipFileCmd.class);
        UnzipFileResponse rsp = new UnzipFileResponse();
        rsp.unzipInstallPath = "/tmp/test-software-package/unzipInstallPath";
        rsp.fileSizes = new java.util.HashMap<>();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = ImageStoreBackupStorageConstant.SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String softwareUpgradePackageDeploy(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        SoftwareUpgradePackageCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SoftwareUpgradePackageCmd.class);
        SoftwareUpgradePackageResponse rsp = new SoftwareUpgradePackageResponse();
        reply(entity, rsp);
        return null;
    }

}
