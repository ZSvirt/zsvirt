package org.zstack.autoscaling.group.instance;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by lining at 2018/9/28
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingGroupInstanceReply extends APIQueryReply {
    private List<AutoScalingGroupInstanceInventory> inventories;

    public List<AutoScalingGroupInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingGroupInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingGroupInstanceReply __example__() {
        APIQueryAutoScalingGroupInstanceReply reply = new APIQueryAutoScalingGroupInstanceReply();
        AutoScalingGroupInstanceInventory inv = new AutoScalingGroupInstanceInventory();

        inv.setUuid(uuid());
        inv.setInstanceUuid(uuid());
        inv.setDescription("just for test");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
