package org.zstack.identity;

import java.sql.Timestamp;

public interface RenewSessionExtensionPoint {
    void renewSession(String sessionUuid, Timestamp expireTime);
}
