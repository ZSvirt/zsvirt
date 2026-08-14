package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;

/**
 * Created by david on 7/22/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ImageStoreBackupStorageKvmDownloader extends BackupStorageSharedBlockKvmDownloader {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;

    private PrimaryStorageInventory pinv;
    private String backupStorageUuid;

    public final static String DOWNLOAD_BIT_PATH = "/sharedblock/imagestore/download";

    public static class DownloadFromImageStoreCmd extends SharedBlockKvmCommands.AgentCmd {
        private String hostname;

        private String username;

        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;
        private int lockType = LvmlockdLockingType.SHARE.getValue();
        private int concurrency = ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY.value(Integer.class);

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

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public int getLockType() {
            return lockType;
        }

        public void setLockType(int lockType) {
            this.lockType = lockType;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }
    }

    private ImageStoreBackupStorageKvmDownloader(PrimaryStorageInventory ps, String bsUuid) {
        this.pinv = ps;
        this.backupStorageUuid = bsUuid;
    }

    public static ImageStoreBackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return new ImageStoreBackupStorageKvmDownloader(ps, bsUuid);
    }

    public void downloadBits(String bsPath, String psPath, LvmlockdLockingType type, Completion completion) {
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
                DownloadFromImageStoreCmd cmd = new DownloadFromImageStoreCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                cmd.setBackupStorageInstallPath(bsPath);
                cmd.setPrimaryStorageInstallPath(psPath);
                cmd.setLockType(type.getValue());
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY, backupStorageUuid, Integer.class);

                new KvmAgentCommandDispatcher(pinv.getUuid()).go(DOWNLOAD_BIT_PATH, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                        completion.success();
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
