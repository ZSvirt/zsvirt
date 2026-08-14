package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventory")
public class APISetServiceTypeOnHostNetworkInterfaceEvent extends APIEvent {

    private List<HostNetworkInterfaceServiceRefInventory> inventory;

    public APISetServiceTypeOnHostNetworkInterfaceEvent() { super(null); }

    public APISetServiceTypeOnHostNetworkInterfaceEvent(String apiId) {
      super(apiId);
    }

    public List<HostNetworkInterfaceServiceRefInventory> getInventory() {
      return inventory;
    }

    public void setInventory(List<HostNetworkInterfaceServiceRefInventory> inventory) {
        this.inventory = inventory;
    }

    public  static APISetServiceTypeOnHostNetworkInterfaceEvent __example__() {
        APISetServiceTypeOnHostNetworkInterfaceEvent event = new APISetServiceTypeOnHostNetworkInterfaceEvent();
        HostNetworkInterfaceServiceRefInventory inventory = new HostNetworkInterfaceServiceRefInventory();

        inventory.setInterfaceUuid(uuid());
        inventory.setVlanId(10);
        inventory.setServiceType(HostNetworkInterfaceServiceType.BackupNetwork);

        event.setInventory(Collections.singletonList(inventory));
        return event;
    }
}
