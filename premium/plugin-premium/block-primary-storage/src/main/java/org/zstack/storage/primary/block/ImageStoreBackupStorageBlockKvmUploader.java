package org.zstack.storage.primary.block;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.image.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/9 12:49
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ImageStoreBackupStorageBlockKvmUploader extends BackupStorageBlockKvmUploader{
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageBlockKvmUploader.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    private PrimaryStorageInventory pinv;
    private String backupStorageUuid;

    public static String UPLOAD_BIT_PATH = "/block/imagestore/upload";

    public static class UploadToImageStoreCmd extends BlockPrimaryStorageKvmCommandDispatcher.AgentCmd implements HasThreadContext{
        private String hostname;
        private String username;
        private String primaryStorageInstallPath;
        private String imageUuid;
        private String description;

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
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

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getHostname() {
            return hostname;
        }
    }

    public static class UploadToImageStoreResponse extends BlockPrimaryStorageKvmCommandDispatcher.AgentRsp {
        public String primaryStorageInstallPath;

        public String backupStorageInstallPath;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    @Override
    public void uploadBits(String imageUuid, String bsPath, String psPath, ReturnValueCompletion<String> completion) {

    }

    @Override
    public void uploadBits(String imageUuid, String bsUuid, String psPath, String hostUuid, ReturnValueCompletion<String> completion) {
        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(this.backupStorageUuid);

        UploadToImageStoreCmd cmd = new UploadToImageStoreCmd();
        logger.debug(String.format("start upload bits %s to %s", imageUuid, bsUuid));
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                logger.debug(String.format("get gmsg"));
                if (!reply.isSuccess()) {
                    logger.debug(String.format(reply.getError().toString()));
                    completion.fail(reply.getError());
                    return;
                }
                final GetImageStoreBackupStorageDownloadCredentialReply greply = reply.castReply();
                cmd.setPrimaryStorageInstallPath(psPath);
                cmd.setImageUuid(imageUuid);
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                StringBuilder desc = new StringBuilder();

                if (imageUuid != null) {
                    logger.debug(String.format("image uuis is not null"));
                    ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(imageUuid, ImageVO.class));
                    for (CreateImageExtensionPoint ext : pluginRegistry.getExtensionList(CreateImageExtensionPoint.class)) {
                        String tmp = ext.getImageDescription(inv);
                        if (tmp != null && !tmp.trim().equals("")) {
                            desc.append(tmp);
                        }
                    }
                    cmd.setDescription(desc.toString());
                    logger.debug(String.format("upload image on host"));
                    BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                    blockPrimaryStorageKvmCommandDispatcher.uploadImage(UPLOAD_BIT_PATH, hostUuid, cmd, UploadToImageStoreResponse.class, new ReturnValueCompletion<UploadToImageStoreResponse>(completion) {
                        @Override
                        public void success(UploadToImageStoreResponse returnValue) {
                            logger.debug(String.format("upload successfully return %s", returnValue.backupStorageInstallPath));
                            completion.success(returnValue.getBackupStorageInstallPath());
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("upload successfully but return fail:%n%s", errorCode.getReadableDetails()));
                            completion.fail(errorCode);
                        }
                    });
                }
            }
        });
    };

    public ImageStoreBackupStorageBlockKvmUploader(PrimaryStorageInventory ps, String bsUuid) {
        this.backupStorageUuid = bsUuid;
        this.pinv = ps;
    };

    public static ImageStoreBackupStorageBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return new ImageStoreBackupStorageBlockKvmUploader(ps, bsUuid);
    }
}