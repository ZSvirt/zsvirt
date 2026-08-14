package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Create by lining at 2018/9/16
 */
@RestResponse(allTo = "inventory")
public class APICreateAutoScalingRuleTriggerEvent extends APIEvent {
    private AutoScalingRuleTriggerInventory inventory;

    public AutoScalingRuleTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingRuleTriggerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateAutoScalingRuleTriggerEvent(String apiId) {
        super(apiId);
    }

    public APICreateAutoScalingRuleTriggerEvent() {
        super(null);
    }

    public static APICreateAutoScalingRuleTriggerEvent __example__() {
        APICreateAutoScalingRuleTriggerEvent evt = new APICreateAutoScalingRuleTriggerEvent();

        AutoScalingRuleTriggerInventory inv = new AutoScalingRuleTriggerInventory();
        inv.setUuid(uuid());
        inv.setName("test-group");
        inv.setDescription("just for test");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventory(inv);
        return evt;
    }
}
