package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListIpRangeReply extends APIReply {
    private List<IpRangeInventory> inventories;

    public List<IpRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IpRangeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListIpRangeReply __example__() {
        APIListIpRangeReply reply = new APIListIpRangeReply();


        return reply;
    }

}
