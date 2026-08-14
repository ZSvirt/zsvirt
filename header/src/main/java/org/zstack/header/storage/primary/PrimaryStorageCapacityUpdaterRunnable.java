package org.zstack.header.storage.primary;

/**
 * Created by frank on 10/19/2015.
 */
public interface PrimaryStorageCapacityUpdaterRunnable {
    PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap);
}
