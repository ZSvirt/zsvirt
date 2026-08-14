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
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class SftpBackupStorageKvmDownloader extends BackupStorageSharedBlockKvmDownloader {
    @Autowired
    private CloudBus bus;

    private String bsUuid;
    private PrimaryStorageInventory pinv;

    private SftpBackupStorageKvmDownloader(PrimaryStorageInventory ps, String bsUuid) {
        this.pinv = ps;
        this.bsUuid = bsUuid;
    }

    public static SftpBackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return new SftpBackupStorageKvmDownloader(ps, bsUuid);
    }

    @Override
    public void downloadBits(final String bsPath, final String psPath, LvmlockdLockingType type, final Completion completion) {
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

                final GetSftpBackupStorageDownloadCredentialReply greply = reply.castReply();
                SharedBlockKvmCommands.SftpDownloadBitsCmd cmd = new SharedBlockKvmCommands.SftpDownloadBitsCmd();
                cmd.hostname = greply.getHostname();
                cmd.username = greply.getUsername();
                cmd.sshKey = greply.getSshKey();
                cmd.sshPort = greply.getSshPort();
                cmd.lockType = type.getValue();
                cmd.backupStorageInstallPath = bsPath;
                cmd.primaryStorageInstallPath = psPath;

                new KvmAgentCommandDispatcher(pinv.getUuid()).go(SharedBlockKvmCommands.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
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
