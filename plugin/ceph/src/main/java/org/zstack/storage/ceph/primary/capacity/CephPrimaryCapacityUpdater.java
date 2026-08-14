package org.zstack.storage.ceph.primary.capacity;

import org.zstack.storage.ceph.CephCapacity;

/**
 * Created by lining on 2021/1/22.
 */
public interface CephPrimaryCapacityUpdater {
    String getCephManufacturer();

    void update(CephCapacity capacity);
}
