package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class SftpBackupStorageKvmUploader extends BackupStorageSharedBlockKvmUploader {
    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    private final String bsUuid;
    private PrimaryStorageInventory pinv;
    private SftpBackupStorageKvmUploader(PrimaryStorageInventory ps, String bsUuid) {
        this.pinv = ps;
        this.bsUuid = bsUuid;
    }


    @Override
    public void uploadBits(final String imageUuid, final String bsPath, final String psPath, final ReturnValueCompletion<String> completion) {
        uploadBits(imageUuid, bsPath, psPath, null, completion);
    }

    public static SftpBackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return new SftpBackupStorageKvmUploader(ps, bsUuid);
    }

    @Override
    public void uploadBits(final String imageUuid, final String bsPath, final String psPath, String hostUuid, final ReturnValueCompletion<String> completion) {
        GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsUuid);
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetSftpBackupStorageDownloadCredentialReply r = reply.castReply();
                SharedBlockKvmCommands.SftpUploadBitsCmd cmd = new SharedBlockKvmCommands.SftpUploadBitsCmd();
                cmd.primaryStorageInstallPath = psPath;
                cmd.backupStorageInstallPath = bsPath;
                cmd.hostname = r.getHostname();
                cmd.username = r.getUsername();
                cmd.sshKey = r.getSshKey();
                cmd.sshPort = r.getSshPort();

                KvmAgentCommandDispatcher dispatcher = hostUuid == null ? new KvmAgentCommandDispatcher(pinv.getUuid()) : new KvmAgentCommandDispatcher(pinv.getUuid(), hostUuid);
                dispatcher.go(SharedBlockKvmCommands.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                        completion.success(bsPath);
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
