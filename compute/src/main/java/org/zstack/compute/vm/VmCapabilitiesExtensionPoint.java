package org.zstack.compute.vm;

import org.zstack.header.vm.VmCapabilities;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by kayo on 2018/10/25.
 */
public interface VmCapabilitiesExtensionPoint {
    void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities);
}
