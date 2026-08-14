package org.zstack.storage.ceph;


/**
 * Created by frank on 7/28/2015.
 */
public interface CephCapacityUpdateExtensionPoint {
    void update(CephCapacity cephCapacity);
}
