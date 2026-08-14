package org.zstack.header.vm;

import org.zstack.header.configuration.InstanceOfferingInventory;

/**
 * Created by frank on 11/19/2015.
 */
public interface ChangeInstanceOfferingExtensionPoint {
    void preChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering);

    void beforeChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering);

    void afterChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering);
}
