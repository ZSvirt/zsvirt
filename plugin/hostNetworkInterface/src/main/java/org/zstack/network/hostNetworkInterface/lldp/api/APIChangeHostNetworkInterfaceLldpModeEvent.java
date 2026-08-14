package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory;
import org.zstack.utils.CollectionDSL;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIChangeHostNetworkInterfaceLldpModeEvent extends APIEvent {
    private List<HostNetworkInterfaceLldpInventory> inventories;

    public APIChangeHostNetworkInterfaceLldpModeEvent() { }

    public APIChangeHostNetworkInterfaceLldpModeEvent(String apiId) {
        super(apiId);
    }

    public List<HostNetworkInterfaceLldpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostNetworkInterfaceLldpInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIChangeHostNetworkInterfaceLldpModeEvent __example__() {
        APIChangeHostNetworkInterfaceLldpModeEvent event = new APIChangeHostNetworkInterfaceLldpModeEvent();
        HostNetworkInterfaceLldpInventory inv = new HostNetworkInterfaceLldpInventory();

        inv.setInterfaceUuid(uuid());
        inv.setMode("rx_only");

        event.setInventories(CollectionDSL.list(inv));
        return event;
    }
}
