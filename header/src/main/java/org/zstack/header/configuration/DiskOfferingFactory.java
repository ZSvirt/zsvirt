package org.zstack.header.configuration;

public interface DiskOfferingFactory {
    DiskOfferingType getDiskOfferingType();

    DiskOfferingInventory createDiskOffering(DiskOfferingVO vo, APICreateDiskOfferingMsg msg);

    DiskOffering getDiskOffering(DiskOfferingVO vo);
}
