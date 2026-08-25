package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.host.HostConstant;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.DownloadExternalPrimaryStorageEncryptedImageMsg;
import org.zstack.header.storage.addon.primary.PrepareExternalPrimaryStorageEncryptedImageDownloadMsg;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.storage.backup.imagestore.CleanupImageNbdChainMsg;
import org.zstack.storage.backup.imagestore.ExportImageNbdChainMsg;
import org.zstack.storage.backup.imagestore.ExportImageNbdChainReply;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageManager;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.Serializable;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class ZbsVolumeBackupImageDownloadHandler {
    private static final CLogger logger = Utils.getLogger(ZbsVolumeBackupImageDownloadHandler.class);
    private static final String ZBS_PRIMARY_STORAGE_TYPE = "zbs";
    private static final String LUKS_CIPHER = "luks";
    private static final String KVM_HOST_IMAGESTORE_ENCRYPTED_DOWNLOAD_PATH =
            "/zbs/primarystorage/kvmhost/imagestore/encrypteddownload";

    @Autowired
    private CloudBus bus;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;
    @Autowired
    private BackupImageKeyManager temporaryImageKeyManager;
    @Autowired
    private ImageStoreBackupStorageManager imageStoreBackupStorageManager;

    public void handle(PrepareExternalPrimaryStorageEncryptedImageDownloadMsg msg) {
        MessageReply reply = new MessageReply();
        if (!ZBS_PRIMARY_STORAGE_TYPE.equals(msg.getPrimaryStorageType())) {
            reply.setError(operr("unsupported external primary storage type[%s] for encrypted image download",
                    msg.getPrimaryStorageType()));
            bus.reply(msg, reply);
            return;
        }

        if (StringUtils.isBlank(msg.getImageUuid())) {
            reply.setError(operr("image uuid is required for encrypted ZBS ImageStore download"));
            bus.reply(msg, reply);
            return;
        }

        String backupUuid = findBackupUuid(msg.getBackupStorageUuid(),
                msg.getBackupStorageInstallPath(), msg.getImageUuid());
        if (StringUtils.isBlank(backupUuid)) {
            reply.setError(operr(
                    "cannot find volume backup for encrypted ZBS ImageStore image[uuid:%s] download",
                    msg.getImageUuid()));
            bus.reply(msg, reply);
            return;
        }

        ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> keyResult =
                temporaryImageKeyManager.prepareTemporaryImageKeyValue(backupUuid, msg.getImageUuid());
        if (!keyResult.isSuccess()) {
            reply.setError(keyResult.error);
            bus.reply(msg, reply);
            return;
        }
        if (keyResult.result == null) {
            reply.setError(operr(
                    "temporary image[uuid:%s] requires encrypted ZBS ImageStore download but has no prepared key",
                    msg.getImageUuid()));
            bus.reply(msg, reply);
            return;
        }

        bus.reply(msg, reply);
    }

    public void handle(DownloadExternalPrimaryStorageEncryptedImageMsg msg) {
        MessageReply reply = new MessageReply();
        if (!ZBS_PRIMARY_STORAGE_TYPE.equals(msg.getPrimaryStorageType())) {
            reply.setError(operr("unsupported external primary storage type[%s] for encrypted image download",
                    msg.getPrimaryStorageType()));
            bus.reply(msg, reply);
            return;
        }

        String backupUuid = findBackupUuid(msg.getBackupStorageUuid(),
                msg.getBackupStorageInstallPath(), msg.getImageUuid());
        if (StringUtils.isBlank(backupUuid)) {
            reply.setError(operr(
                    "cannot find volume backup for encrypted ZBS ImageStore image[uuid:%s] download",
                    msg.getImageUuid()));
            bus.reply(msg, reply);
            return;
        }

        downloadToEncryptedTarget(msg, backupUuid, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void downloadToEncryptedTarget(DownloadExternalPrimaryStorageEncryptedImageMsg msg, String backupUuid,
                                           Completion completion) {
        boolean sourceEncrypted = temporaryImageKeyManager.isBackupEncrypted(backupUuid);
        if (StringUtils.isBlank(msg.getRemoteTargetUrl())) {
            completion.fail(operr(
                    "remote target is required for encrypted ZBS ImageStore download of image[uuid:%s]",
                    msg.getImageUuid()));
            return;
        }

        ErrorableValue<String> host = imageStoreBackupStorageManager.findConnectedKvmHost(
                msg.getPrimaryStorageUuid(),
                "zbs primary storage",
                "download encrypted ImageStore image");
        if (!host.isSuccess()) {
            completion.fail(host.error);
            return;
        }

        ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> keyResult =
                temporaryImageKeyManager.prepareTemporaryImageKeyValue(backupUuid, msg.getImageUuid());
        if (!keyResult.isSuccess()) {
            completion.fail(keyResult.error);
            return;
        }
        if (keyResult.result == null) {
            completion.fail(operr(
                    "temporary image[uuid:%s] requires encrypted ZBS ImageStore download but has no prepared key",
                    msg.getImageUuid()));
            return;
        }

        ErrorableValue<String> encryptedDek = volumeEncryptedSecretHelper.verifyHostKeyAndHpkeSealDekErrorable(
                host.result, msg.getImageUuid(), keyResult.result.getDekBase64());
        if (!encryptedDek.isSuccess()) {
            completion.fail(encryptedDek.error);
            return;
        }

        String installPath = msg.getPrimaryStorageInstallPath();
        if (StringUtils.isBlank(installPath)) {
            completion.fail(operr(
                    "primary storage install path is required for encrypted ZBS ImageStore download of image[uuid:%s]",
                    msg.getImageUuid()));
            return;
        }

        exportImageStoreNbdChain(msg, new ReturnValueCompletion<ExportImageNbdChainReply>(completion) {
            @Override
            public void success(ExportImageNbdChainReply exported) {
                KVMHostImageStoreEncryptedDownloadCmd cmd = new KVMHostImageStoreEncryptedDownloadCmd();
                cmd.psUuid = msg.getPrimaryStorageUuid();
                cmd.backupStorageHostname = exported.getHostname();
                cmd.backupStorageNbdPort = exported.getPort();
                cmd.backupStorageNbdExportNames = exported.getExportNames();
                cmd.backupStorageInstallPath = msg.getBackupStorageInstallPath();
                cmd.primaryStorageInstallPath = installPath;
                cmd.keyProviderUuid = keyResult.result.getKeyProviderUuid();
                cmd.keyVersion = keyResult.result.getKeyVersion();
                cmd.cipher = LUKS_CIPHER;
                cmd.sourceEncrypted = sourceEncrypted;
                cmd.encryptedDek = encryptedDek.result;

                httpCallToKvmHost(host.result, KVM_HOST_IMAGESTORE_ENCRYPTED_DOWNLOAD_PATH, cmd,
                        KVMHostLuksRsp.class, new ReturnValueCompletion<KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(KVMHostLuksRsp returnValue) {
                        completeAfterNbdCleanup(msg, exported.getSessionId(), null, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completeAfterNbdCleanup(msg, exported.getSessionId(), errorCode, completion);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void exportImageStoreNbdChain(DownloadExternalPrimaryStorageEncryptedImageMsg downloadMsg,
                                          ReturnValueCompletion<ExportImageNbdChainReply> completion) {
        ExportImageNbdChainMsg msg = new ExportImageNbdChainMsg();
        msg.setBackupStorageUuid(downloadMsg.getBackupStorageUuid());
        msg.setImageUuid(downloadMsg.getImageUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID,
                downloadMsg.getBackupStorageUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                ExportImageNbdChainReply er = reply.castReply();
                if (StringUtils.isBlank(er.getHostname()) || StringUtils.isBlank(er.getSessionId()) ||
                        er.getPort() <= 0 || er.getExportNames() == null || er.getExportNames().isEmpty()) {
                    completion.fail(operr(
                            "backup storage[uuid:%s] returned incomplete NBD chain export for encrypted ZBS ImageStore download",
                            downloadMsg.getBackupStorageUuid()));
                    return;
                }
                completion.success(er);
            }
        });
    }

    private void cleanupImageStoreNbdChain(DownloadExternalPrimaryStorageEncryptedImageMsg downloadMsg,
                                           String sessionId,
                                           Completion completion) {
        CleanupImageNbdChainMsg msg = new CleanupImageNbdChainMsg();
        msg.setBackupStorageUuid(downloadMsg.getBackupStorageUuid());
        msg.setSessionId(sessionId);
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID,
                downloadMsg.getBackupStorageUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    private void completeAfterNbdCleanup(DownloadExternalPrimaryStorageEncryptedImageMsg downloadMsg,
                                         String sessionId,
                                         ErrorCode downloadError, Completion completion) {
        cleanupImageStoreNbdChain(downloadMsg, sessionId, new Completion(completion) {
            @Override
            public void success() {
                if (downloadError == null) {
                    completion.success();
                } else {
                    completion.fail(downloadError);
                }
            }

            @Override
            public void fail(ErrorCode cleanupError) {
                if (downloadError == null) {
                    completion.fail(cleanupError);
                } else {
                    logger.warn(String.format("failed to clean ImageStore NBD chain session[%s]: %s",
                            sessionId, cleanupError));
                    completion.fail(downloadError);
                }
            }
        });
    }

    private <T extends KVMAgentCommands.AgentResponse> void httpCallToKvmHost(
            final String hostUuid,
            final String path,
            final Object cmd,
            final Class<T> retClass,
            final ReturnValueCompletion<T> callback) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setPath(path);
        kmsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(callback) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    callback.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                T rsp = kr.toResponse(retClass);
                if (rsp == null) {
                    callback.fail(operr(
                            "kvm host[uuid:%s] returned null reply for path[%s]",
                            hostUuid, path));
                    return;
                }
                if (!rsp.isSuccess()) {
                    callback.fail(operr(
                            "kvm host[uuid:%s] path[%s] failed",
                            hostUuid, path).withException(rsp.getError()));
                    return;
                }
                callback.success(rsp);
            }
        });
    }

    private String findBackupUuid(String backupStorageUuid, String backupStorageInstallPath, String imageUuid) {
        String backupUuid = imageStoreBackupStorageManager.findVolumeBackupUuid(
                backupStorageUuid, backupStorageInstallPath);
        return StringUtils.isNotBlank(backupUuid) ?
                backupUuid : temporaryImageKeyManager.findBackupUuidByImageUuid(imageUuid);
    }

    private static class KVMHostImageStoreEncryptedDownloadCmd implements Serializable {
        public String psUuid;
        public String backupStorageHostname;
        public int backupStorageNbdPort;
        public List<String> backupStorageNbdExportNames;
        public String backupStorageInstallPath;
        public String primaryStorageInstallPath;
        public String keyProviderUuid;
        public Integer keyVersion;
        public String cipher;
        public boolean sourceEncrypted;
        @NoLogging
        public String encryptedDek;
    }

    private static class KVMHostLuksRsp extends KVMAgentCommands.AgentResponse {
        public Long actualSize;
    }
}
