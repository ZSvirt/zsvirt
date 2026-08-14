package org.zstack.core.config;

/**
 * @Author: DaoDao
 * @Date: 2021/11/19
 */
public interface AfterUpdateClobalConfigExtensionPoint {
    void saveSaveEncryptAfterUpdateClobalConfig(GlobalConfigInventory inventory);
}
