package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryUplinkGroupReply extends APIQueryReply {
    private List<UplinkGroupInventory> inventories;

    public List<UplinkGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UplinkGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryUplinkGroupReply __example__() {
        APIQueryUplinkGroupReply reply = new APIQueryUplinkGroupReply();
        UplinkGroupInventory inv = new UplinkGroupInventory();

        inv.setInterfaceName("eth0");
        inv.setL2NetworkUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setType(UplinkGroupType.PhysicalInterface);
        inv.setInterfaceUuid(uuid());

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
