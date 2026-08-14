package org.zstack.header.storage.primary;

/**
 * Created by frank on 10/21/2015.
 */
public interface RecalculatePrimaryStorageCapacityExtensionPoint {
    String getPrimaryStorageTypeForRecalculateCapacityExtensionPoint();

    void afterRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct);

    void beforeRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct);
}
