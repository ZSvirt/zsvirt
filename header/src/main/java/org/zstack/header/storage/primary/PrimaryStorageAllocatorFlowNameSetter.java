package org.zstack.header.storage.primary;

import java.util.List;

/**
 * Created by frank on 9/17/2015.
 */
public interface PrimaryStorageAllocatorFlowNameSetter {
    List<String> getAllocatorFlowNames();

    void setAllocatorFlowNames(List<String> flowNames);
}
