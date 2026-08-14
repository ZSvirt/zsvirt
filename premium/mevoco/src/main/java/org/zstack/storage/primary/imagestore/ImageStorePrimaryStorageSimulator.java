package org.zstack.storage.primary.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.primary.imagestore.local.LocalStorageKvmImageStoreBackupStorageMediatorImpl;
import org.zstack.storage.primary.imagestore.nfs.NfsPrimaryStorageImageStoreBackend;
import org.zstack.storage.primary.imagestore.nfs.NfsPrimaryToImageStoreBackupKVMBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by david on 8/6/16.
 */
public class ImageStorePrimaryStorageSimulator {
    CLogger logger = Utils.getLogger(ImageStorePrimaryStorageSimulator.class);

    @Autowired
    private ImageStorePrimaryStorageSimulatorConfig config;
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

    @RequestMapping(value = NfsPrimaryStorageImageStoreBackend.COMMIT_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String nfsCommitImage(HttpEntity<String> entity) {
        NfsPrimaryStorageImageStoreBackend.CommitVolumeAsImageCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), NfsPrimaryStorageImageStoreBackend.CommitVolumeAsImageCmd.class);
        NfsPrimaryStorageImageStoreBackend.CommitVolumeAsImageRsp rsp =
                new NfsPrimaryStorageImageStoreBackend.CommitVolumeAsImageRsp();
        rsp.setActualSize(2000);
        rsp.setBackupStorageInstallPath(String.format("zstore://%s/deadbeef", cmd.getImageUuid()));
        rsp.setSize(1000);
        rsp.setSuccess(true);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = NfsPrimaryToImageStoreBackupKVMBackend.DOWNLOAD_FROM_IMAGESTORE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String nfsDownloadFromImageStore(HttpEntity<String> entity) {
        NfsPrimaryToImageStoreBackupKVMBackend.DownloadFromImageStoreCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), NfsPrimaryToImageStoreBackupKVMBackend.DownloadFromImageStoreCmd.class);
        LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreDownloadBitsRsp  rsp =
                new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreDownloadBitsRsp();

        long usedSize = 0;
        for (Long s : config.imageSizes.values()) {
            usedSize += s;
        }
        rsp.setTotalCapacity(config.totalCapacity);
        rsp.setAvailableCapacity(config.totalCapacity - usedSize);
        rsp.setSuccess(true);
        logger.debug(String.format("Downloaded %s", cmd.getBackupStorageInstallPath()));
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = NfsPrimaryToImageStoreBackupKVMBackend.UPLOAD_TO_IMAGESTORE_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String nfsUploadToImageStore(HttpEntity<String> entity) {
        LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsCmd.class);

        LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsRsp rsp =
                new LocalStorageKvmImageStoreBackupStorageMediatorImpl.ImageStoreUploadBitsRsp();

        long usedSize = 0;
        for (Long s : config.imageSizes.values()) {
            usedSize += s;
        }
        rsp.setTotalCapacity(config.totalCapacity - usedSize);
        rsp.setTotalCapacity(config.totalCapacity);
        rsp.setSuccess(true);
        rsp.backupStorageInstallPath = "zstore://test-image/ac19e2e37f935c3b6a86500d0ddf2bf6cb1bfc9f";
        logger.debug(String.format("Uploaded %s to %s", cmd.getPrimaryStorageInstallPath(), rsp.backupStorageInstallPath));
        reply(entity, rsp);
        return null;
    }
}
