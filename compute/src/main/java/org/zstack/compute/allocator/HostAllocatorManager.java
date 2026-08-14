package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorStrategyFactory;
import org.zstack.header.allocator.HostAllocatorStrategyType;

import java.util.List;
import java.util.Map;

public interface HostAllocatorManager {
    HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type);

    Map<String, List<String>> getBackupStoragePrimaryStorageMetrics();

    List<String> getPrimaryStorageTypesByBackupStorageTypeFromMetrics(String backupStorageType);

    List<String> getBackupStorageTypesByPrimaryStorageTypeFromMetrics(String psType);

    void returnComputeResourceCapacity(String uuid, long cpu, long memory);
}
