package org.zstack.header.storage.primary;

import org.zstack.header.tag.SystemTagInventory;

import java.util.List;

/**
 */
public interface PrimaryStorageTagAllocatorExtensionPoint {
    List<PrimaryStorageVO> allocatePrimaryStorage(List<SystemTagInventory> tags, List<PrimaryStorageVO> candidates);
}
