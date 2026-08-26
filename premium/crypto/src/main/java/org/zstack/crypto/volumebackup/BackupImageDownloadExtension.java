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
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.imagestore.ImageStoreDownloadContext;
import org.zstack.header.storage.primary.imagestore.ImageStoreDownloadExtension;
import org.zstack.header.storage.primary.imagestore.ceph.CephImageStoreDownloadContext;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageManager;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;

import static org.zstack.core.Platform.operr;

public class BackupImageDownloadExtension extends ImageStoreDownloadExtension {
    private static final String LUKS_CIPHER = "luks";

    @Autowired
    private CloudBus bus;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;
    @Autowired
    private BackupImageKeyManager temporaryImageKeyManager;
    @Autowired
    private ImageStoreBackupStorageManager imageStoreBackupStorageManager;

    @Override
    public void afterDownload(ImageStoreDownloadContext context, Completion completion) {
        String imageUuid = findImageUuid(context);
        String backupUuid = findBackupUuid(context, imageUuid);
        if (StringUtils.isBlank(imageUuid) && temporaryImageKeyManager.isBackupEncrypted(backupUuid)) {
            completion.fail(operr(
                    "cannot find temporary image uuid for encrypted backup[uuid:%s] download from backup storage[uuid:%s, path:%s]",
                    backupUuid, context.getBackupStorageUuid(), context.getBackupStorageInstallPath()));
            return;
        }
        if (!supports(backupUuid, imageUuid)) {
            completion.success();
            return;
        }

        if (temporaryImageKeyManager.isBackupEncrypted(backupUuid)) {
            ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> keyResult =
                    temporaryImageKeyManager.prepareTemporaryImageKeyValue(backupUuid, imageUuid);
            if (!keyResult.isSuccess()) {
                completion.fail(keyResult.error);
                return;
            }
            completion.success();
            return;
        }

        String hostUuid = context.getHostUuid();
        if (StringUtils.isBlank(hostUuid)) {
            ErrorableValue<String> host = imageStoreBackupStorageManager.findConnectedKvmHost(
                    context.getPrimaryStorageUuid(),
                    "primary storage",
                    "encrypt downloaded ImageStore image");
            if (!host.isSuccess()) {
                completion.fail(host.error);
                return;
            }
            hostUuid = host.result;
        }

        ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> keyResult =
                temporaryImageKeyManager.prepareTemporaryImageKeyValue(backupUuid, imageUuid);
        if (!keyResult.isSuccess()) {
            completion.fail(keyResult.error);
            return;
        }
        if (keyResult.result == null) {
            completion.fail(operr(
                    "temporary image[uuid:%s] requires encrypted ImageStore download but has no prepared key",
                    imageUuid));
            return;
        }

        ErrorableValue<String> encryptedDek = volumeEncryptedSecretHelper.verifyHostKeyAndHpkeSealDekErrorable(
                hostUuid, imageUuid, keyResult.result.getDekBase64());
        if (!encryptedDek.isSuccess()) {
            completion.fail(encryptedDek.error);
            return;
        }

        EncryptVolumeBitsOnPrimaryStorageMsg msg = new EncryptVolumeBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(context.getPrimaryStorageUuid());
        msg.setHostUuid(hostUuid);
        msg.setVolumeUuid(imageUuid);
        msg.setInstallPath(context.getPrimaryStorageInstallPath());
        msg.setEncryptedDek(encryptedDek.result);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, context.getPrimaryStorageUuid());
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

    @Override
    public boolean supports(CephImageStoreDownloadContext context) {
        String backupUuid = findBackupUuid(context);
        String imageUuid = context.getParam().getImage().getInventory().getUuid();
        return supports(backupUuid, imageUuid);
    }

    @Override
    public void download(CephImageStoreDownloadContext context, Completion completion) {
        String imageUuid = context.getParam().getImage().getInventory().getUuid();
        String backupUuid = findBackupUuid(context);
        boolean sourceEncrypted = temporaryImageKeyManager.isBackupEncrypted(backupUuid);

        ErrorableValue<String> host = imageStoreBackupStorageManager.findConnectedKvmHost(
                context.getPrimaryStorage().getUuid(),
                "ceph primary storage",
                "download encrypted ImageStore image");
        if (!host.isSuccess()) {
            completion.fail(host.error);
            return;
        }

        ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> keyResult =
                temporaryImageKeyManager.prepareTemporaryImageKeyValue(backupUuid, imageUuid);
        if (!keyResult.isSuccess()) {
            completion.fail(keyResult.error);
            return;
        }
        if (keyResult.result == null) {
            completion.fail(operr(
                    "temporary image[uuid:%s] requires encrypted Ceph ImageStore download but has no prepared key",
                    imageUuid));
            return;
        }

        ErrorableValue<String> encryptedDek = volumeEncryptedSecretHelper.verifyHostKeyAndHpkeSealDekErrorable(
                host.result, imageUuid, keyResult.result.getDekBase64());
        if (!encryptedDek.isSuccess()) {
            completion.fail(encryptedDek.error);
            return;
        }

        CephPrimaryStorageBase.KVMHostImageStoreEncryptedDownloadCmd cmd =
                new CephPrimaryStorageBase.KVMHostImageStoreEncryptedDownloadCmd();
        cmd.psUuid = context.getPrimaryStorage().getUuid();
        cmd.hostname = StringUtils.isNotEmpty(context.getBackupStorageHostname()) ?
                context.getBackupStorageHostname() : context.getBackupStorage().hostname;
        cmd.backupStorageInstallPath = context.getParam().getImage().getSelectedBackupStorage().getInstallPath();
        cmd.primaryStorageInstallPath = context.getParam().getInstallPath();
        cmd.concurrency = context.getConcurrency();
        cmd.keyProviderUuid = keyResult.result.getKeyProviderUuid();
        cmd.keyVersion = keyResult.result.getKeyVersion();
        cmd.cipher = LUKS_CIPHER;
        cmd.sourceEncrypted = sourceEncrypted;
        cmd.encryptedDek = encryptedDek.result;

        httpCallToKvmHost(host.result,
                CephPrimaryStorageBase.KVM_HOST_IMAGESTORE_ENCRYPTED_DOWNLOAD_PATH,
                cmd, CephPrimaryStorageBase.KVMHostLuksRsp.class,
                new ReturnValueCompletion<CephPrimaryStorageBase.KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(CephPrimaryStorageBase.KVMHostLuksRsp returnValue) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private boolean supports(String backupUuid, String imageUuid) {
        return StringUtils.isNotBlank(backupUuid) &&
                StringUtils.isNotBlank(imageUuid) &&
                temporaryImageKeyManager.shouldHandleDownload(backupUuid, imageUuid);
    }

    private String findImageUuid(ImageStoreDownloadContext context) {
        String imageUuid = imageStoreBackupStorageManager.findImageUuid(
                context.getBackupStorageUuid(), context.getBackupStorageInstallPath());
        if (StringUtils.isNotBlank(imageUuid)) {
            return imageUuid;
        }

        return null;
    }

    private String findBackupUuid(ImageStoreDownloadContext context, String imageUuid) {
        String backupUuid = imageStoreBackupStorageManager.findVolumeBackupUuid(
                context.getBackupStorageUuid(), context.getBackupStorageInstallPath());
        return StringUtils.isNotBlank(backupUuid) ? backupUuid : temporaryImageKeyManager.findBackupUuidByImageUuid(imageUuid);
    }

    private String findBackupUuid(CephImageStoreDownloadContext context) {
        String backupUuid = imageStoreBackupStorageManager.findVolumeBackupUuid(
                context.getParam().getImage().getSelectedBackupStorage().getBackupStorageUuid(),
                context.getParam().getImage().getSelectedBackupStorage().getInstallPath());
        if (StringUtils.isNotBlank(backupUuid)) {
            return backupUuid;
        }

        return temporaryImageKeyManager.findBackupUuidByImageUuid(
                context.getParam().getImage().getInventory().getUuid());
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
}
