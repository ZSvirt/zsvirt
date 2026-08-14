package org.zstack.compute.host;

/**
 * Created by kayo on 2018/11/27.
 */
public class HostMaintenancePolicyManagerImpl implements HostMaintenancePolicyManager {
    @Override
    public HostMaintenancePolicy getHostMaintenancePolicy(String hostUuid) {
        return HostMaintenancePolicy.valueOf(HostGlobalConfig.HOST_MAINTENANCE_POLICY.value());
    }
}
