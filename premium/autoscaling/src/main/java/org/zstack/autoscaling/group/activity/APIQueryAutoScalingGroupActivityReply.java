package org.zstack.autoscaling.group.activity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by lining at 2018/9/28
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingGroupActivityReply extends APIQueryReply {
    private List<AutoScalingGroupActivityInventory> inventories;

    public List<AutoScalingGroupActivityInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingGroupActivityInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingGroupActivityReply __example__() {
        APIQueryAutoScalingGroupActivityReply reply = new APIQueryAutoScalingGroupActivityReply();
        AutoScalingGroupActivityInventory inv = new AutoScalingGroupActivityInventory();

        inv.setUuid(uuid());
        inv.setDescription("just for test");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
