package org.zstack.header.vm;

public interface VmNicSetDriverExtensionPoint {
    String getPreferredVmNicDriver(VmInstanceInventory vm);
}