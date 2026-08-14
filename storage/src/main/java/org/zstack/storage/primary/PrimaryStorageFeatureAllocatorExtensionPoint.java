package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageFeature;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;
import java.util.Set;

/**
 * @ Author : yh.w
 * @ Date   : Created in 10:49 2025/7/15
 */
public interface PrimaryStorageFeatureAllocatorExtensionPoint {
    List<PrimaryStorageVO> allocatePrimaryStorage(Set<PrimaryStorageFeature> requiredFeatures, List<PrimaryStorageVO> candidates);
}
