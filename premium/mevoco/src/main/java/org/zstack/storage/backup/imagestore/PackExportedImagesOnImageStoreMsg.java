package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.util.List;

/**
 * Created by Qi Le on 2022/4/29
 */
public class PackExportedImagesOnImageStoreMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    private List<String> imageUuids;

    // the format of exported image. e.g. vmdk
    private String imageExportFormat;

    private String configFileContent;

    // the format of config file. e.g. ovf
    private String configFileFormat;

    private String packageName;

    // the format of the whole image package. e.g. ova
    private String packageFormat;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public List<String> getImageUuids() {
        return imageUuids;
    }

    public void setImageUuids(List<String> imageUuids) {
        this.imageUuids = imageUuids;
    }

    public String getImageExportFormat() {
        return imageExportFormat;
    }

    public void setImageExportFormat(String imageExportFormat) {
        this.imageExportFormat = imageExportFormat;
    }

    public String getConfigFileContent() {
        return configFileContent;
    }

    public void setConfigFileContent(String configFileContent) {
        this.configFileContent = configFileContent;
    }

    public String getConfigFileFormat() {
        return configFileFormat;
    }

    public void setConfigFileFormat(String configFileFormat) {
        this.configFileFormat = configFileFormat;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageFormat() {
        return packageFormat;
    }

    public void setPackageFormat(String packageFormat) {
        this.packageFormat = packageFormat;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
