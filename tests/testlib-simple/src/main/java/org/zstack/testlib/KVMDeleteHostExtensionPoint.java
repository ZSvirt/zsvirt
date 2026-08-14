package org.zstack.testlib;

import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;

public class KVMDeleteHostExtensionPoint implements HostDeleteExtensionPoint {
    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {

    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        KVMSimulator.getConnectCmdConcurrentHashMap().remove(inventory.getUuid());
    }
}
