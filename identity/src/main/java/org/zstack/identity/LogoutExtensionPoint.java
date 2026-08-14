package org.zstack.identity;

import org.zstack.header.identity.SessionInventory;

/**
 * @Author: DaoDao
 * @Date: 2023/8/31
 */
public interface LogoutExtensionPoint {
    void beforeLogout(SessionInventory session);
}
