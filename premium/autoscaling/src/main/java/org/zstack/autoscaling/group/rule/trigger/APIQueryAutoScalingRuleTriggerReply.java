package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by lining at 2018/10/10
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingRuleTriggerReply extends APIQueryReply {
    private List<AutoScalingRuleTriggerInventory> inventories;

    public List<AutoScalingRuleTriggerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingRuleTriggerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingRuleTriggerReply __example__() {
        APIQueryAutoScalingRuleTriggerReply reply = new APIQueryAutoScalingRuleTriggerReply();
        AutoScalingRuleTriggerInventory inv = new AutoScalingRuleTriggerInventory();

        inv.setUuid(uuid());
        inv.setName("test-load-balance-profile");
        inv.setDescription("just for test");
        inv.setType(AutoScalingConstants.AutoScalingRule.TriggerType.Alarm);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
