package org.zstack.header.identity;

/**
 * Created by xing5 on 2016/5/17.
 */
public interface SessionLogoutExtensionPoint {
    void sessionLogout(SessionInventory session);
}
