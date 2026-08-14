package org.zstack.header.zql;

import org.zstack.header.identity.SessionInventory;

import java.util.List;

public interface ZQLQueryExtensionPoint {
    void beforeQueryExtensionPoint(List<Class> queryTargetInventoryClass, SessionInventory session);
}
