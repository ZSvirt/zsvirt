package org.zstack.storage.ceph.primary.capacity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.storage.ceph.CephCapacity;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephPoolCapacity;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ZStoneCephPrimaryCapacityBaseUpdater extends EnterpriseCephPrimaryCapacityBaseUpdater {
    @Override
    public String getCephManufacturer() {
        return CephConstants.CEPH_MANUFACTURER_ZSTONE;
    }
}
