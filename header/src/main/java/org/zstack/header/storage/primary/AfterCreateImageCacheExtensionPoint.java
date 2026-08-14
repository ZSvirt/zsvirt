package org.zstack.header.storage.primary;


import org.zstack.header.core.Completion;

/**
 * @Author: DaoDao
 * @Date: 2021/11/16
 */
public interface AfterCreateImageCacheExtensionPoint {
    void saveEncryptAfterCreateImageCache(String hostUuid, ImageCacheInventory inventory, Completion completion);

    void checkEncryptImageCache(String hostUuid, ImageCacheInventory inventory, Completion completion);
}
