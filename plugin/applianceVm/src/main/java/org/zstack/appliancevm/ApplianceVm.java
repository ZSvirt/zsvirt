package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstance;

import java.util.List;

/**
 */
public interface ApplianceVm extends VmInstance {
    List<String> getSnatL3NetworkOnRouter(String vrUuid);
    void detachNetworkService(String vrUuid, String networkServiceType, String l3NetworkUuid);
    void attachNetworkService(String vrUuid, String networkServiceType, String l3NetworkUuid);
}
