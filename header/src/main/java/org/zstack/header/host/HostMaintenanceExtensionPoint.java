package org.zstack.header.host;

import java.util.List;

public interface HostMaintenanceExtensionPoint {
    void beforeCheckMaintenancePolicy(List<String> operateVmUuids);
}
