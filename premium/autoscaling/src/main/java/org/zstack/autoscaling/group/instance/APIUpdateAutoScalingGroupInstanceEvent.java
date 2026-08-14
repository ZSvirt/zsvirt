package org.zstack.autoscaling.group.instance;

import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Create by lining at 2020/03/19
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAutoScalingGroupInstanceEvent extends APIEvent {
    private AutoScalingGroupInstanceInventory inventory;

    public AutoScalingGroupInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingGroupInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAutoScalingGroupInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateAutoScalingGroupInstanceEvent() {
        super(null);
    }

    public static APIUpdateAutoScalingGroupInstanceEvent __example__() {
        APIUpdateAutoScalingGroupInstanceEvent evt = new APIUpdateAutoScalingGroupInstanceEvent();
        AutoScalingGroupInstanceInventory inv = new AutoScalingGroupInstanceInventory();

        inv.setUuid(uuid());
        inv.setInstanceUuid(uuid());
        inv.setScalingGroupUuid(uuid());
        inv.setDescription("just for test");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setProtectionStrategy(AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_PROTECTED);
        return evt;
    }
}
