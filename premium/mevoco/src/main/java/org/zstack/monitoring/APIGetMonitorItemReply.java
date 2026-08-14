package org.zstack.monitoring;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.monitoring.items.ItemInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/28.
 */
@RestResponse(allTo = "inventories")
public class APIGetMonitorItemReply extends APIReply {
    private List<ItemInventory> inventories;

    public List<ItemInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ItemInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMonitorItemReply __example__() {
        APIGetMonitorItemReply reply = new APIGetMonitorItemReply();
        ItemInventory inv = new ItemInventory();
        inv.setName("host.cpu.util");
        inv.setReadableName("Host CPU Utilization");
        reply.setInventories(asList(inv));
        return reply;
    }
}
