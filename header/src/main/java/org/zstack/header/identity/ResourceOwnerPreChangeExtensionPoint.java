package org.zstack.header.identity;

/**
 * Created by xing5 on 2016/5/20.
 */
public interface ResourceOwnerPreChangeExtensionPoint {
    void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid);
}
