package org.zstack.compute.host;

/**
 * Created by kayo on 2018/11/27.
 */
public interface HostMaintenancePolicyManager {
    enum HostMaintenancePolicy {
        JustMigrate,
        StopVmOnMigrationFailure
    }

    HostMaintenancePolicy getHostMaintenancePolicy(String hostUuid);
}
