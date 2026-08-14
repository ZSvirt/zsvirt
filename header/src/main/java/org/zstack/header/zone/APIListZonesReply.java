package org.zstack.header.zone;

import org.zstack.header.message.APIReply;

import java.util.List;


public class APIListZonesReply extends APIReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }

 
    public static APIListZonesReply __example__() {
        APIListZonesReply reply = new APIListZonesReply();
        //deprecated
        return reply;
    }

}
