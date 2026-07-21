package org.zstack.crypto.volumebackup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.core.encrypt.CryptoServiceConstant;
import org.zstack.header.message.Message;
import org.zstack.header.storage.addon.primary.DownloadExternalPrimaryStorageEncryptedImageMsg;
import org.zstack.header.storage.addon.primary.PrepareExternalPrimaryStorageEncryptedImageDownloadMsg;

public class CryptoVolumeBackupService extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ZbsVolumeBackupImageDownloadHandler zbsVolumeBackupImageDownloadHandler;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof PrepareExternalPrimaryStorageEncryptedImageDownloadMsg) {
            zbsVolumeBackupImageDownloadHandler.handle((PrepareExternalPrimaryStorageEncryptedImageDownloadMsg) msg);
        } else if (msg instanceof DownloadExternalPrimaryStorageEncryptedImageMsg) {
            zbsVolumeBackupImageDownloadHandler.handle((DownloadExternalPrimaryStorageEncryptedImageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CryptoServiceConstant.SERVICE_ID);
    }
}
