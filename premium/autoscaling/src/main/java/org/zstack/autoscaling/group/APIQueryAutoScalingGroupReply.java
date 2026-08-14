package org.zstack.autoscaling.group;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingGroupReply extends APIQueryReply {
    private List<AutoScalingGroupInventory> inventories;

    public List<AutoScalingGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingGroupReply __example__() {
        APIQueryAutoScalingGroupReply reply = new APIQueryAutoScalingGroupReply();
        AutoScalingGroupInventory inv = new AutoScalingGroupInventory();

        inv.setUuid(uuid());
        inv.setName("test-group2");
        inv.setDescription("just for test");
        //inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
