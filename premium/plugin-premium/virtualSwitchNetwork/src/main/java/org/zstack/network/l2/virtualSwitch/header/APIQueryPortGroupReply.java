package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPortGroupReply extends APIQueryReply {
    private List<PortGroupInventory> inventories;

    public List<PortGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PortGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPortGroupReply __example__() {
        APIQueryPortGroupReply reply = new APIQueryPortGroupReply();
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

        reply.setInventories(Collections.singletonList(pg));
        return reply;
    }
}
