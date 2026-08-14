package org.zstack.header.host;

import java.util.Map;
import java.util.Set;

/**
 * Created by frank on 10/25/2015.
 */
public interface HostMaintenancePolicyExtensionPoint {
    public static enum HostMaintenancePolicy {
        MigrateVm,
        StopVm
    }

    Map<String, HostMaintenancePolicy> getHostMaintenanceVmOperationPolicy(HostInventory host);
}
