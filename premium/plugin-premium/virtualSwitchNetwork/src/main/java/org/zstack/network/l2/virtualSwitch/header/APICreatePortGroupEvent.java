package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l3.APICreateL3NetworkEvent;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreatePortGroupEvent extends APICreateL3NetworkEvent {
    public APICreatePortGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreatePortGroupEvent() {
        super(null);
    }

    public static APICreatePortGroupEvent __example__() {
        APICreatePortGroupEvent event = new APICreatePortGroupEvent();
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
        pg.setEnableIPAM(Boolean.FALSE);
        pg.setvSwitchUuid(uuid());
        pg.setvSwitchUuid(uuid());
        pg.setVlanId(100);
        pg.setVlanMode(PortGroupVlanMode.ACCESS);

        event.setInventory(pg);
        return event;
    }
}
