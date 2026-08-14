package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 17/2/21.
 */
public class ExportImageFromBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String imageUuid;
    private String rawPath;
    private String exportFormat;
    private Long requiredSize;
    private String imageName;
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

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public String getRawPath() {
        return rawPath;
    }

    public void setRawPath(String rawPath) {
        this.rawPath = rawPath;
    }

    public Long getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(Long requiredSize) {
        this.requiredSize = requiredSize;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
