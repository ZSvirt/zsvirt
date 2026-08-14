package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageKvmSftpBackupStorageMediatorImpl implements LocalStorageBackupStorageMediator {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private DatabaseFacade dbf;

    public static final String UPLOAD_BIT_PATH = "/localstorage/sftp/upload";
    public static final String DOWNLOAD_BIT_PATH = "/localstorage/sftp/download";

    public static class SftpDownloadBitsCmd extends LocalStorageKvmBackend.AgentCommand implements HasThreadContext {
        private String sshKey;
        private String username;
        private String hostname;
        private int sshPort;
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public int getSshPort() {
            return sshPort;
        }
        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }
        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
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
    }

    public static class SftpDownloadBitsRsp extends LocalStorageKvmBackend.AgentResponse {

    }

    public static class SftpUploadBitsCmd extends LocalStorageKvmBackend.AgentCommand implements HasThreadContext{
        private String primaryStorageInstallPath;
        private String backupStorageInstallPath;
        private String hostname;
        private String sshKey;
        private String username;
        private String imageName;
        private int sshPort;
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public int getSshPort() {
            return sshPort;
        }
        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
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

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }
    }

    public static class SftpUploadBitsRsp extends LocalStorageKvmBackend.AgentResponse {

    }

    public void downloadBits(final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, boolean isData, final Completion completion) {
        GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetSftpBackupStorageDownloadCredentialReply greply = reply.castReply();
                SftpDownloadBitsCmd cmd = new SftpDownloadBitsCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                cmd.setSshKey(greply.getSshKey());
                cmd.setSshPort(greply.getSshPort());
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.storagePath =  pinv.getUrl();

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setPath(DOWNLOAD_BIT_PATH);
                msg.setCommand(cmd);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        SftpDownloadBitsRsp rsp = kr.toResponse(SftpDownloadBitsRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("failed to download bits from the SFTP backup storage[hostname:%s, path: %s] to the local primary storage[uuid:%s, path: %s], %s",
                                    greply.getHostname(), backupStorageInstallPath, pinv.getUuid(), primaryStorageInstallPath, rsp.getError()));
                        } else {
                            completion.success();
                        }
                    }
                });
            }
        });
    }

    public void uploadBits(final String imageUuid, final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, final ReturnValueCompletion<String> completion) {
        GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetSftpBackupStorageDownloadCredentialReply r = reply.castReply();
                SftpUploadBitsCmd cmd = new SftpUploadBitsCmd();
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setHostname(r.getHostname());
                cmd.setSshKey(r.getSshKey());
                cmd.setSshPort(r.getSshPort());
                cmd.setUsername(r.getUsername());
                cmd.storagePath = pinv.getUrl();

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(UPLOAD_BIT_PATH);
                msg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        SftpUploadBitsRsp rsp = kr.toResponse(SftpUploadBitsRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("failed to upload bits from the local storage[uuid:%s, path:%s] to the SFTP backup storage[hostname:%s, path:%s], %s",
                                            pinv.getUuid(), primaryStorageInstallPath, r.getHostname(), backupStorageInstallPath, rsp.getError()));
                            return;
                        }

                        completion.success(backupStorageInstallPath);
                    }
                });
            }
        });
    }

    @Override
    public String getSupportedPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public String getSupportedBackupStorageType() {
        return SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE;
    }

    @Override
    public List<String> getSupportedHypervisorTypes() {
        return list(KVMConstant.KVM_HYPERVISOR_TYPE);
    }
}
