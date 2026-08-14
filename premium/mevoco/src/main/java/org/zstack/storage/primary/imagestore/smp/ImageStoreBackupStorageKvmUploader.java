package org.zstack.storage.primary.imagestore.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;
import org.zstack.storage.primary.smp.BackupStorageKvmUploader;
import org.zstack.storage.primary.smp.KvmAgentCommandDispatcher;
import org.zstack.storage.primary.smp.KvmBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by david on 7/22/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ImageStoreBackupStorageKvmUploader extends BackupStorageKvmUploader {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageKvmUploader.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ResourceConfigFacade rcf;

    private PrimaryStorageInventory pinv;
    private String backupStorageUuid;

    public final static String UPLOAD_BIT_PATH = "/sharedmountpointprimarystorage/imagestore/upload";

    public static class UploadToImageStoreCmd extends KvmBackend.AgentCmd implements HasThreadContext{
        private String hostname;
        private String username;
        private String primaryStorageInstallPath;
        private String imageUuid;
        private String description;
        private int concurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }
    }

    public static class UploadToImageStoreResponse extends KvmBackend.AgentRsp {

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        private String backupStorageInstallPath;
    }

    private ImageStoreBackupStorageKvmUploader(PrimaryStorageInventory ps, String bsUuid) {
        this.pinv = ps;
        this.backupStorageUuid = bsUuid;
    }

    public static ImageStoreBackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return new ImageStoreBackupStorageKvmUploader(ps, bsUuid);
    }

    public void uploadBits(String imageUuid, String bsPath, String psPath, ReturnValueCompletion<String> completion) {
        ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(imageUuid, ImageVO.class));

        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(this.backupStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, this.backupStorageUuid);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetImageStoreBackupStorageDownloadCredentialReply greply = reply.castReply();
                UploadToImageStoreCmd cmd = new UploadToImageStoreCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                cmd.setPrimaryStorageInstallPath(psPath);
                cmd.setImageUuid(imageUuid);
                StringBuilder desc = new StringBuilder();
                for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
                    String tmp = ext.getImageDescription(inv);
                    if (tmp != null && !tmp.trim().equals("")) {
                        desc.append(tmp);
                    }
                }
                cmd.setDescription(desc.toString());
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY, backupStorageUuid, Integer.class);

                new KvmAgentCommandDispatcher(pinv.getUuid()).go(UPLOAD_BIT_PATH, cmd, UploadToImageStoreResponse.class, new ReturnValueCompletion<UploadToImageStoreResponse>(completion) {
                    @Override
                    public void success(UploadToImageStoreResponse returnValue) {
                        completion.success(returnValue.getBackupStorageInstallPath());
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }
}
