package org.zstack.storage.ceph.primary.capacity;

import org.zstack.storage.ceph.CephConstants;

/**
 * Created by lining on 2021/1/22.
 */
public class XSKYCephPrimaryCapacityBaseUpdater extends EnterpriseCephPrimaryCapacityBaseUpdater {
    @Override
    public String getCephManufacturer() {
        return CephConstants.CEPH_MANUFACTURER_XSKY;
    }
}
