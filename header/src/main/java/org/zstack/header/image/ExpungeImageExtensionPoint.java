package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 9/16/2015.
 */
public interface ExpungeImageExtensionPoint {
    void preExpungeImage(ImageInventory img);

    void beforeExpungeImage(ImageInventory img);

    void afterExpungeImage(ImageInventory img, String imageBackupStorageUuid);

    void failedToExpungeImage(ImageInventory img, ErrorCode err);
}
