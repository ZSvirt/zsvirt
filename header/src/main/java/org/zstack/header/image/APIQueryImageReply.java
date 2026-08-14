package org.zstack.header.image;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryImageReply extends APIQueryReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryImageReply __example__() {
        APIQueryImageReply reply = new APIQueryImageReply();


        return reply;
    }

}
