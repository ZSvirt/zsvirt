package org.zstack.compute.vm;

public interface VmMigrationMetric {
    boolean isCapable(String rootVolumePrimaryStorageType);
    boolean isSupportWithSharedBlock();
}
