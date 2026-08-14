package org.zstack.header.vm;


public interface NicManageExtensionPoint {
    void beforeCreateNic(VmNicInventory nic, APICreateVmNicMsg msg);

    void beforeDeleteNic(VmNicInventory nic);
}
