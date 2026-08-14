package org.zstack.appliancevm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;

/**
 */
public class ApplianceVmPostLifeCycleInfo {
    private VmNicInventory managementNic;
    private L3NetworkInventory defaultRouteL3Network;

    public VmNicInventory getManagementNic() {
        return managementNic;
    }

    public void setManagementNic(VmNicInventory managementNic) {
        this.managementNic = managementNic;
    }

    public L3NetworkInventory getDefaultRouteL3Network() {
        return defaultRouteL3Network;
    }

    public void setDefaultRouteL3Network(L3NetworkInventory defaultRouteL3Network) {
        this.defaultRouteL3Network = defaultRouteL3Network;
    }
}
