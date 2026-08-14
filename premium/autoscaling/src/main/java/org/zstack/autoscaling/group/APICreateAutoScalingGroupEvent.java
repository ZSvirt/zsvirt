package org.zstack.autoscaling.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventory")
public class APICreateAutoScalingGroupEvent extends APIEvent {
    private AutoScalingGroupInventory inventory;

    public AutoScalingGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateAutoScalingGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateAutoScalingGroupEvent() {
        super(null);
    }

    public static APICreateAutoScalingGroupEvent __example__() {
        APICreateAutoScalingGroupEvent evt = new APICreateAutoScalingGroupEvent();

        AutoScalingGroupInventory inv = new AutoScalingGroupInventory();
        inv.setUuid(uuid());
        inv.setName("test-group");
        inv.setDescription("just for test");
        //inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventory(inv);
        return evt;
    }
}
