package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageInventory;

public class MediatorUploadParam implements MediatorParam {
    public ImageInventory image;
    public String primaryStorageUuid;
    public String primaryStorageInstallPath;
    public String backupStorageInstallPath;
    public String backupStorageUuid;
}
