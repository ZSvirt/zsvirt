package org.zstack.ha.fencers;

import org.zstack.header.tag.SystemTagInventory;

public interface KvmFencer {
    String getName();
    String vmFencedReason(SystemTagInventory fencedByTag);
}
