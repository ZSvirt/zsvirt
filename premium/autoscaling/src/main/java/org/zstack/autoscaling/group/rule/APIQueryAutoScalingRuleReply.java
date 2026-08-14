package org.zstack.autoscaling.group.rule;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingRuleReply extends APIQueryReply {
    private List<AutoScalingRuleInventory> inventories;

    public List<AutoScalingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingRuleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingRuleReply __example__() {
        APIQueryAutoScalingRuleReply reply = new APIQueryAutoScalingRuleReply();
        AddingNewVmRuleInventory inv = new AddingNewVmRuleInventory();

        inv.setUuid(uuid());
        inv.setName("test-load-balance-profile");
        inv.setDescription("just for test");
        inv.setType(AutoScalingRuleConstants.LOAD_BALANCE_PROFILE);
        inv.setState(AutoScalingRuleState.Enabled);
        inv.setStatus(AutoScalingRuleStatus.Created);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCooldown(100l);

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
