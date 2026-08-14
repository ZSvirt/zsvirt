package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APIProvisionVirtualRouterConfigEvent extends APIEvent {
    private ApplianceVmInventory inventory;

    public APIProvisionVirtualRouterConfigEvent(String apiId) {
        super(apiId);
    }

    public APIProvisionVirtualRouterConfigEvent() {
        super(null);
    }

    public ApplianceVmInventory getInventory() {
        return inventory;
    }

    public void setInventory(ApplianceVmInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIProvisionVirtualRouterConfigEvent __example__() {
        APIProvisionVirtualRouterConfigEvent event = new APIProvisionVirtualRouterConfigEvent();
        ApplianceVmInventory vm = new ApplianceVmInventory();

        vm.setManagementNetworkUuid(uuid());
        vm.setName("Test-Router");
        vm.setDescription("this is a virtual router vm");
        vm.setClusterUuid(uuid());
        vm.setImageUuid(uuid());
        vm.setInstanceOfferingUuid(uuid());
        vm.setManagementNetworkUuid(uuid());

        event.setInventory(vm);
        return event;
    }

}
