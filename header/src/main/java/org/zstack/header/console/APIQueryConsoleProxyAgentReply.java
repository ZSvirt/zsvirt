package org.zstack.header.console;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/3/15.
 */
@RestResponse(allTo = "inventories")
public class APIQueryConsoleProxyAgentReply extends APIQueryReply {
    private List<ConsoleProxyAgentInventory> inventories;

    public List<ConsoleProxyAgentInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ConsoleProxyAgentInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryConsoleProxyAgentReply __example__() {
        APIQueryConsoleProxyAgentReply reply = new APIQueryConsoleProxyAgentReply();
        reply.setInventories(list(ConsoleProxyAgentInventory.__example__()));
        return reply;
    }
}
