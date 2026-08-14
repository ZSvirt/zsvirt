package org.zstack.storage.backup.sftp;

import org.zstack.header.image.ImageInventory;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 12/21/16.
 */
public class SftpBackupStorageDumpMetadataInfo {
    private ImageInventory img;
    private Boolean dumpAllInfo;
    private String backupStorageUrl = null;
    private String backupStorageHostname = null;
    private String backupStorageUuid = null;

    public ImageInventory getImg() {
        return img;
    }

    public void setImg(ImageInventory img) {
        this.img = img;
    }

    public Boolean getDumpAllInfo() {
        return dumpAllInfo;
    }

    public void setDumpAllInfo(Boolean dumpAllInfo) {
        this.dumpAllInfo = dumpAllInfo;
    }

    public String getBackupStorageUrl() {
        return backupStorageUrl;
    }

    public void setBackupStorageUrl(String backupStorageUrl) {
        this.backupStorageUrl = backupStorageUrl;
    }

    public String getBackupStorageHostname() {
        return backupStorageHostname;
    }

    public void setBackupStorageHostname(String backupStorageHostname) {
        this.backupStorageHostname = backupStorageHostname;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
