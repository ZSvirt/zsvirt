package org.zstack.header.zql;

import org.zstack.header.identity.SessionInventory;

public interface ZQLExtensionContext {
    String getQueryTargetInventoryName();

    SessionInventory getAPISession();
}
