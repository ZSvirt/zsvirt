package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateVirtualSwitchUplinkGroupEvent extends APIEvent {
    private UplinkGroupInventory inventory;

    public APIUpdateVirtualSwitchUplinkGroupEvent() {
        super(null);
    }

    public APIUpdateVirtualSwitchUplinkGroupEvent(String apiId) {
        super(apiId);
    }

    public UplinkGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(UplinkGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVirtualSwitchUplinkGroupEvent __example__ () {
        APIUpdateVirtualSwitchUplinkGroupEvent event = new APIUpdateVirtualSwitchUplinkGroupEvent();
        UplinkGroupInventory inv = new UplinkGroupInventory();

        inv.setInterfaceName("eth0");
        inv.setL2NetworkUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setType(UplinkGroupType.PhysicalInterface);
        inv.setInterfaceUuid(uuid());

        event.setInventory(inv);
        return event;
    }

}
