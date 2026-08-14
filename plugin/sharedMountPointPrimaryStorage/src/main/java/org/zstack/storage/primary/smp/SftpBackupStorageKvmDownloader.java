package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:14 2023/7/7
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class SftpBackupStorageKvmDownloader extends BackupStorageKvmDownloader {
    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    private PrimaryStorageInventory pinv;
    private String bsUuid;

    public static final String DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointprimarystorage/sftp/download";

    public SftpBackupStorageKvmDownloader(PrimaryStorageInventory ps, String backupStorageUuid) {
        pinv = ps;
        bsUuid = backupStorageUuid;
    }

    public static SftpBackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return new SftpBackupStorageKvmDownloader(ps, bsUuid);
    }

    @Override
    public void downloadBits(final String bsPath, final String psPath, boolean isData, final Completion completion) {
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
                KvmBackend.SftpDownloadBitsCmd cmd = new KvmBackend.SftpDownloadBitsCmd();
                cmd.hostname = greply.getHostname();
                cmd.username = greply.getUsername();
                cmd.sshKey = greply.getSshKey();
                cmd.sshPort = greply.getSshPort();
                cmd.backupStorageInstallPath = bsPath;
                cmd.primaryStorageInstallPath = psPath;
                cmd.primaryStorageUuid = pinv.getUuid();

                new KvmAgentCommandDispatcher(pinv.getUuid()).go(DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, cmd, KvmBackend.AgentRsp.class, new ReturnValueCompletion<KvmBackend.AgentRsp>(completion) {
                    @Override
                    public void success(KvmBackend.AgentRsp returnValue) {
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
