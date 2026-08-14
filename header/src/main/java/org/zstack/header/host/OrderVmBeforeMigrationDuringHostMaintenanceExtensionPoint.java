package org.zstack.header.host;

import java.util.List;

/**
 * Created by xing5 on 2016/4/21.
 */
public interface OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint {
    List<String> orderVmBeforeMigrationDuringHostMaintenance(HostInventory host, List<String> vmUuids);
}
