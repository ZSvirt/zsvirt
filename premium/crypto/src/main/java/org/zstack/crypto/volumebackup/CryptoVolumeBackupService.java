package org.zstack.crypto.volumebackup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.encrypt.CryptoServiceConstant;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.DownloadExternalPrimaryStorageEncryptedImageMsg;
import org.zstack.header.storage.addon.primary.PrepareExternalPrimaryStorageEncryptedImageDownloadMsg;
import org.zstack.header.volume.ConvertVolumeBackupEncryptionMsg;
import org.zstack.header.volume.ConvertVolumeBackupEncryptionReply;
import org.zstack.header.volume.DeleteVolumeBackupConversionPlaceholdersMsg;
import org.zstack.header.volume.RefreshVolumeBackupMetadataMsg;
import org.zstack.header.volume.ValidateVolumeBackupEncryptionConversionMsg;
import org.zstack.header.volume.VolumeBackupEncryptionConversionResult;

public class CryptoVolumeBackupService extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ZbsVolumeBackupImageDownloadHandler zbsVolumeBackupImageDownloadHandler;
    @Autowired
    private VolumeBackupEncryptionConversion volumeBackupEncryptionConversion;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof PrepareExternalPrimaryStorageEncryptedImageDownloadMsg) {
            zbsVolumeBackupImageDownloadHandler.handle((PrepareExternalPrimaryStorageEncryptedImageDownloadMsg) msg);
        } else if (msg instanceof DownloadExternalPrimaryStorageEncryptedImageMsg) {
            zbsVolumeBackupImageDownloadHandler.handle((DownloadExternalPrimaryStorageEncryptedImageMsg) msg);
        } else if (msg instanceof ValidateVolumeBackupEncryptionConversionMsg) {
            handle((ValidateVolumeBackupEncryptionConversionMsg) msg);
        } else if (msg instanceof ConvertVolumeBackupEncryptionMsg) {
            handle((ConvertVolumeBackupEncryptionMsg) msg);
        } else if (msg instanceof RefreshVolumeBackupMetadataMsg) {
            handle((RefreshVolumeBackupMetadataMsg) msg);
        } else if (msg instanceof DeleteVolumeBackupConversionPlaceholdersMsg) {
            handle((DeleteVolumeBackupConversionPlaceholdersMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ValidateVolumeBackupEncryptionConversionMsg msg) {
        MessageReply reply = new MessageReply();
        ErrorCode errorCode = volumeBackupEncryptionConversion.validate(msg.getVolume(), msg.isTargetEncrypted());
        if (errorCode != null) {
            reply.setError(errorCode);
        }
        bus.reply(msg, reply);
    }

    private void handle(ConvertVolumeBackupEncryptionMsg msg) {
        ConvertVolumeBackupEncryptionReply reply = new ConvertVolumeBackupEncryptionReply();
        volumeBackupEncryptionConversion.convert(msg.getVolume(), msg.isTargetEncrypted(),
                new ReturnValueCompletion<VolumeBackupEncryptionConversionResult>(msg) {
                    @Override
                    public void success(VolumeBackupEncryptionConversionResult result) {
                        reply.setResult(result);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void handle(RefreshVolumeBackupMetadataMsg msg) {
        MessageReply reply = new MessageReply();
        volumeBackupEncryptionConversion.refreshBackupMetadata(msg.getBackupUuids());
        bus.reply(msg, reply);
    }

    private void handle(DeleteVolumeBackupConversionPlaceholdersMsg msg) {
        MessageReply reply = new MessageReply();
        volumeBackupEncryptionConversion.deleteConversionPlaceholders(msg.getPlaceholderBackupUuids());
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CryptoServiceConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
