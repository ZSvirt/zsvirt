package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.RemoteTargetProtocol;

/**
 * Created by mingjian.deng on 17/2/21.
 */
public class DeleteExportedImageFromImageStoreBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String imageUuid;
    private String rawPath;
    private String exportFormat;
    private RemoteTargetProtocol targetProtocol = RemoteTargetProtocol.HTTP;

    public RemoteTargetProtocol getTargetProtocol() {
        return targetProtocol;
    }

    public void setTargetProtocol(RemoteTargetProtocol targetProtocol) {
        this.targetProtocol = targetProtocol;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getRawPath() {
        return rawPath;
    }

    public void setRawPath(String rawPath) {
        this.rawPath = rawPath;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }
}
