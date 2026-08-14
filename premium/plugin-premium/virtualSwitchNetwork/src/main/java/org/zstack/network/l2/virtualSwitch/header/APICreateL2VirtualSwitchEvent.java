package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin.ruan 2023.09.01
 */
@RestResponse(allTo = "inventory")
public class APICreateL2VirtualSwitchEvent extends APICreateL2NetworkEvent {
    public APICreateL2VirtualSwitchEvent(String apiId) {
        super(apiId);
    }

    public APICreateL2VirtualSwitchEvent() {
        super(null);
    }

    public static APICreateL2VirtualSwitchEvent __example__() {
        APICreateL2VirtualSwitchEvent event = new APICreateL2VirtualSwitchEvent();
        L2VirtualSwitchNetworkInventory net = new L2VirtualSwitchNetworkInventory();

        net.setName("dvs-1");
        net.setDescription("Test dvs");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("bond1");
        net.setType(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE);
        net.setDistributed(Boolean.TRUE);
        net.setVSwitchIndex(1);

        event.setInventory(net);
        return event;
    }
}
