package org.zstack.header.identity;

import org.zstack.header.errorcode.ErrorCode;

/**
 * @Author: DaoDao
 * @Date: 2023/2/10
 */
public interface RenewSessionPreAuthExtensionPoint {
    ErrorCode checkAuth(SessionInventory session);
}
