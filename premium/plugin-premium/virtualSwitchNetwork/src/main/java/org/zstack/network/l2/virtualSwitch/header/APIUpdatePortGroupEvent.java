package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l3.APIUpdateL3NetworkEvent;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdatePortGroupEvent extends APIUpdateL3NetworkEvent {

    public APIUpdatePortGroupEvent() {
        super(null);
    }

    public APIUpdatePortGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdatePortGroupEvent __example__() {
        APIUpdatePortGroupEvent event = new APIUpdatePortGroupEvent();
        PortGroupInventory pg = new PortGroupInventory();

        pg.setUuid(uuid());
        pg.setName("port-group-1");
        pg.setDescription("Test");
        pg.setType(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
        pg.setZoneUuid(uuid());
        pg.setL2NetworkUuid(uuid());
        pg.setState(L3NetworkState.Enabled.toString());
        pg.setSystem(Boolean.FALSE);
        pg.setCategory(L3NetworkCategory.Private.toString());
        pg.setvSwitchUuid(uuid());
        pg.setvSwitchUuid(uuid());
        pg.setVlanId(100);
        pg.setVlanMode(PortGroupVlanMode.ACCESS);

        event.setInventory(pg);
        return event;
    }
}
