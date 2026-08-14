package org.zstack.header.image;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;


@RestResponse(allTo = "inventories")
public class APIGetCandidateImagesForCreatingVmReply extends APIReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateImagesForCreatingVmReply __example__() {
        APIGetCandidateImagesForCreatingVmReply reply = new APIGetCandidateImagesForCreatingVmReply();
        return reply;
    }

}
