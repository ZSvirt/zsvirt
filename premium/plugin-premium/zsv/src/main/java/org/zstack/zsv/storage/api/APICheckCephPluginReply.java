package org.zstack.zsv.storage.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.storage.entity.CephPluginConnectionView;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(fieldsTo = {"all"})
public class APICheckCephPluginReply extends APIReply {
    private List<CephPluginConnectionView> inventories;

    public List<CephPluginConnectionView> getInventories() {
        return inventories;
    }

    public void setInventories(List<CephPluginConnectionView> inventories) {
        this.inventories = inventories;
    }

    public static APICheckCephPluginReply __example__() {
        APICheckCephPluginReply reply = new APICheckCephPluginReply();
        reply.setInventories(list(CephPluginConnectionView.__example__()));
        return reply;
    }
}
