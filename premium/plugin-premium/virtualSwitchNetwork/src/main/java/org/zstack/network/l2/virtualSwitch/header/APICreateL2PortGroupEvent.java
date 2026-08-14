package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin.ruan 2023.09.01
 */
@RestResponse(allTo = "inventory")
public class APICreateL2PortGroupEvent extends APICreateL2NetworkEvent {
    public APICreateL2PortGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateL2PortGroupEvent() {
        super(null);
    }

    public static APICreateL2PortGroupEvent __example__() {
        APICreateL2PortGroupEvent event = new APICreateL2PortGroupEvent();
        L2PortGroupNetworkInventory net = new L2PortGroupNetworkInventory();

        net.setName("port-group-1");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setType(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
        net.setvSwitchUuid(uuid());
        net.setVlanId(100);

        event.setInventory(net);
        return event;
    }
}
