package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupSystemTags;
import org.zstack.autoscaling.group.instance.vm.AutoScalingGroupVmInstanceHealthStrategy;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.TagUtils;
import java.sql.Timestamp;
import java.util.Arrays;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventory")
public class APICreateAutoScalingRuleEvent extends APIEvent {
    private AutoScalingRuleInventory inventory;

    public AutoScalingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingRuleInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateAutoScalingRuleEvent(String apiId) {
        super(apiId);
    }

    public APICreateAutoScalingRuleEvent() {
        super(null);
    }

    public static APICreateAutoScalingRuleEvent __example__() {
        APICreateAutoScalingRuleEvent evt = new APICreateAutoScalingRuleEvent();

        AutoScalingRuleInventory inv = new AutoScalingRuleInventory();
        inv.setUuid(uuid());
        inv.setName("test-health-policy");
        inv.setDescription("just for test");
        inv.setCooldown(10l);
        inv.setState(AutoScalingRuleState.Enabled);
        //inv.setStatus(AutoScalingRuleStatus.Running);
        inv.setType(AutoScalingRuleConstants.HEALTH_PROFILE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventory(inv);
        return evt;
    }
}
