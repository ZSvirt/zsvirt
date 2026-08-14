package org.zstack.header.vm;

import java.util.Map;

public interface VmNicChangeNetworkExtensionPoint {
    Map<String, String> getVmNicAttachedNetworkService(VmNicInventory nic);
}
