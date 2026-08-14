package org.zstack.autoscaling.template;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

import static java.util.Arrays.asList;

/**
 * Create by lining at 2018/9/10
 */
@RestResponse(allTo = "inventory")
public class APICreateAutoScalingTemplateEvent extends APIEvent {
    private AutoScalingTemplateInventory inventory;

    public AutoScalingTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateAutoScalingTemplateEvent(String apiId) {
        super(apiId);
    }

    public APICreateAutoScalingTemplateEvent() {
        super(null);
    }

    public static APICreateAutoScalingTemplateEvent __example__() {
        APICreateAutoScalingTemplateEvent evt = new APICreateAutoScalingTemplateEvent();
        AutoScalingTemplateInventory inv = new AutoScalingTemplateInventory();
        inv.setName("template");
        inv.setDescription("desc");
        inv.setState(AutoScalingTemplateState.Enabled.toString());

        evt.setInventory(inv);
        return evt;
    }
}
