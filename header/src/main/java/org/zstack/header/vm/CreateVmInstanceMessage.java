package org.zstack.header.vm;

import java.util.List;

import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Created by david on 8/22/16.
 */
public interface CreateVmInstanceMessage {
    String getInstanceOfferingUuid();

    String getAccountUuid();

    String getName();

    String getImageUuid();

    int getCpuNum();

    long getCpuSpeed();

    long getMemorySize();

    long getReservedMemorySize();

    List<VmNicSpec> getL3NetworkSpecs();

    String getType();

    default String getRootDiskOfferingUuid() {
        final DiskAO bootDisk = getRootDisk();
        return bootDisk == null ? null : bootDisk.getDiskOfferingUuid();
    }

    default long getRootDiskSize() {
        final DiskAO bootDisk = getRootDisk();
        return bootDisk == null ? 0 : bootDisk.getSize();
    }

    String getZoneUuid();

    String getClusterUuid();

    String getHostUuid();

    String getDescription();

    String getResourceUuid();

    String getDefaultL3NetworkUuid();

    String getAllocatorStrategy();

    String getStrategy(); // VmCreationStrategy

    DiskAO getRootDisk();

    List<DiskAO> getDataDisks();
}
