package org.zstack.header.host;

import java.util.Map;

/**
 * Created by frank on 10/25/2015.
 */
public interface HostMaintenancePolicyExtensionPoint {
    enum HostMaintenancePolicy {
        MigrateVm,
        StopVm
    }

    Map<String, HostMaintenancePolicy> getHostMaintenanceVmOperationPolicy(HostInventory host);
}
