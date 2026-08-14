package org.zstack.image;

/**
 * @Author: DaoDao
 * @Date: 2021/11/8
 */
public interface AfterAddImageExtensionPoint {
    void saveEncryptAfterAddImage(String imageUuid);
}
