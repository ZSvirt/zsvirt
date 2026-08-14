package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerTriggerInventory;
import org.zstack.header.vpc.VpcRouterVmInventory;
import org.zstack.scheduler.APIGetAvailableTriggersReply;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIGetRouteTableVpcVRouterCandidateReply extends APIReply {

    private List<VpcRouterVmInventory> inventories;

    public List<VpcRouterVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VpcRouterVmInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetAvailableTriggersReply __example__() {
        APIGetAvailableTriggersReply reply = new APIGetAvailableTriggersReply();

        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setName("trigger");
        inv.setDescription("this is a scheduler trigger");
        inv.setUuid(uuid());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inv));

        return reply;
    }
}

