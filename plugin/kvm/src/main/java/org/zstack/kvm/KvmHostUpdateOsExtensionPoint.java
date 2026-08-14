package org.zstack.kvm;

import org.zstack.header.host.HostInventory;

import java.util.Map;

public interface KvmHostUpdateOsExtensionPoint {
    String UPDATE_OS_RSP = "UPDATE_OS_RSP";

    void afterUpdateOs(Map data, HostInventory host);
}