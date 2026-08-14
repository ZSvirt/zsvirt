package org.zstack.header.configuration;

public interface InstanceOfferingFactory {
    InstanceOfferingType getInstanceOfferingType();

    InstanceOfferingInventory createInstanceOffering(InstanceOfferingVO vo, APICreateInstanceOfferingMsg msg);

    InstanceOffering getInstanceOffering(InstanceOfferingVO vo);
}
