package org.zstack.header.identity;

/**
 * Created by xing5 on 2016/5/20.
 */
public interface ResourceOwnerAfterChangeExtensionPoint {
    void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid);
}
