package org.zstack.zsv.core.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.core.entity.NodeRolesView;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(fieldsTo = {"all"})
public class APIGetNodeRolesReply extends APIReply {
    private List<NodeRolesView> inventories;

    public List<NodeRolesView> getInventories() {
        return inventories;
    }

    public void setInventories(List<NodeRolesView> inventories) {
        this.inventories = inventories;
    }

    public static APIGetNodeRolesReply __example__() {
        APIGetNodeRolesReply reply = new APIGetNodeRolesReply();
        reply.setInventories(list(NodeRolesView.__example__()));
        return reply;
    }
}
