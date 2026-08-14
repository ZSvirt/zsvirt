package org.zstack.autoscaling.template;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2020/02/25
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAutoScalingTemplateEvent extends APIEvent {
    private AutoScalingTemplateInventory inventory;

    public AutoScalingTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAutoScalingTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateAutoScalingTemplateEvent() {
        super(null);
    }

    public static APIUpdateAutoScalingTemplateEvent __example__() {
        APIUpdateAutoScalingTemplateEvent evt = new APIUpdateAutoScalingTemplateEvent();
        AutoScalingTemplateInventory inv = new AutoScalingTemplateInventory();
        inv.setName("template");
        inv.setDescription("desc");
        inv.setState(AutoScalingTemplateState.Enabled.toString());

        evt.setInventory(inv);
        return evt;
    }
}
