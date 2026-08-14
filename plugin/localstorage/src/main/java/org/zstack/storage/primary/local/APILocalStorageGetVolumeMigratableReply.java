package org.zstack.storage.primary.local;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 11/18/2015.
 */
@RestResponse(allTo = "inventories")
public class APILocalStorageGetVolumeMigratableReply extends APIReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APILocalStorageGetVolumeMigratableReply __example__() {
        APILocalStorageGetVolumeMigratableReply reply = new APILocalStorageGetVolumeMigratableReply();


        return reply;
    }

}
