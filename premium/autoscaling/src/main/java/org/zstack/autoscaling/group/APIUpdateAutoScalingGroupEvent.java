package org.zstack.autoscaling.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAutoScalingGroupEvent extends APIEvent {
    private AutoScalingGroupInventory inventory;

    public AutoScalingGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAutoScalingGroupEvent() {
    }

    public APIUpdateAutoScalingGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateAutoScalingGroupEvent __example__() {
        APIUpdateAutoScalingGroupEvent reply = new APIUpdateAutoScalingGroupEvent();
        AutoScalingGroupInventory inv = new AutoScalingGroupInventory();

        inv.setUuid(uuid());
        inv.setName("test-load-balance-profile");
        inv.setDescription("just for test");
        //inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventory(inv);
        return reply;
    }
}
