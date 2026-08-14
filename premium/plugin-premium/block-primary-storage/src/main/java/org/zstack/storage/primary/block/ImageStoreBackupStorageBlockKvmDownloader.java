package org.zstack.storage.primary.block;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialReply;

import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/9 12:52
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ImageStoreBackupStorageBlockKvmDownloader extends BackupStorageBlockKvmDownloader{
    @Autowired
    private CloudBus bus;

    private PrimaryStorageInventory pinv;
    private String backupStorageUuid;

    public final static String DOWNLOAD_BIT_PATH = "/block/imagestore/download";

    public static class DownloadFromImageStoreCmd {
        public String uuid;
        private String hostname;
        private String username;
        private String wwn;

        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getWwn() {
            return wwn;
        }

        public void setWwn(String wwn) {
            this.wwn = wwn;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public ImageStoreBackupStorageBlockKvmDownloader() {
    }

    public class DownloadBitsFromBackupStorageResponse extends KVMAgentCommands.AgentResponse {
    }

    public ImageStoreBackupStorageBlockKvmDownloader(PrimaryStorageInventory ps, String bsUuid) {
        this.pinv = ps;
        this.backupStorageUuid = bsUuid;
    }

    public static ImageStoreBackupStorageBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return new ImageStoreBackupStorageBlockKvmDownloader(ps, bsUuid);
    }

    @Override
    public void downloadBits(String bsPath, String psPath, String hostUuid, Completion completion) {
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
                cmd.setUuid(pinv.getUuid());
                cmd.setBackupStorageInstallPath(bsPath);
                cmd.setPrimaryStorageInstallPath(psPath);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setPath(DOWNLOAD_BIT_PATH);
                msg.setNoStatusCheck(false);
                msg.setCommand(cmd);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }
                        DownloadBitsFromBackupStorageResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(DownloadBitsFromBackupStorageResponse.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("failed to download[%s] from BackupStorage[hostname:%s] to block primary storage[uuid:%s, path:%s], %s",
                                    bsPath, greply.getHostname(), pinv.getUuid(), psPath, rsp.getError()));
                            return;
                        }
                        completion.success();
                    }
                });
            }
        });
    }
}