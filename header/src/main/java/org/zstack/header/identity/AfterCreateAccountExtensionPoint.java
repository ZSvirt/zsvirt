package org.zstack.header.identity;

/**
 * Created by frank on 2/26/2016.
 */
public interface AfterCreateAccountExtensionPoint {
    void afterCreateAccount(AccountInventory account);
}
