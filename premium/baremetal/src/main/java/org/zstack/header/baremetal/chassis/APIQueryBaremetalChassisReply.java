package org.zstack.header.baremetal.chassis;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestResponse(allTo = "inventories")
public class APIQueryBaremetalChassisReply extends APIQueryReply {
    private List<BaremetalChassisInventory> inventories;

    public List<BaremetalChassisInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BaremetalChassisInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryBaremetalChassisReply __example__() {
        APIQueryBaremetalChassisReply reply = new APIQueryBaremetalChassisReply();
        BaremetalChassisInventory inv = new BaremetalChassisInventory();
        inv.setUuid(uuid());
        inv.setIpmiAddress("1.1.1.1");
        inv.setIpmiPort(623);
        inv.setIpmiUsername("root");
        inv.setIpmiPassword("password");
        inv.setState(BaremetalChassisState.Enabled.toString());
        inv.setStatus(BaremetalChassisStatus.Available.toString());
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
