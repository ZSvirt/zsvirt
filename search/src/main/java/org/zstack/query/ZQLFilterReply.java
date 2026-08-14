package org.zstack.query;

import java.util.List;

public interface ZQLFilterReply {
    List<String> getInventoryUuids();

    void setFilteredInventories(List inventories);

    String getInventoryName();
}
