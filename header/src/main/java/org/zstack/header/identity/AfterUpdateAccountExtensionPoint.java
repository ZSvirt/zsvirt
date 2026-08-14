package org.zstack.header.identity;

/**
 * @description:
 * @author: liupt@sugon.com
 * @time: 2022/10/9
 */
public interface AfterUpdateAccountExtensionPoint {
    void afterUpdateAccount(AccountInventory account);
}
