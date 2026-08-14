package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventory")
public class APISetServiceTypeOnHostNetworkBondingEvent extends APIEvent {

    private List<HostNetworkBondingServiceRefInventory> inventory;

    public APISetServiceTypeOnHostNetworkBondingEvent() { super(null); }

    public APISetServiceTypeOnHostNetworkBondingEvent(String apiId) {
      super(apiId);
    }

    public List<HostNetworkBondingServiceRefInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<HostNetworkBondingServiceRefInventory> inventory) {
        this.inventory = inventory;
    }

    public  static APISetServiceTypeOnHostNetworkBondingEvent __example__() {
        APISetServiceTypeOnHostNetworkBondingEvent event = new APISetServiceTypeOnHostNetworkBondingEvent();
        HostNetworkBondingServiceRefInventory inventory = new HostNetworkBondingServiceRefInventory();

        inventory.setBondingUuid(uuid());
        inventory.setVlanId(10);
        inventory.setServiceType(HostNetworkInterfaceServiceType.BackupNetwork);

        event.setInventory(Arrays.asList(inventory));
        return event;
    }
}
