package org.zstack.autoscaling.template;

import org.zstack.autoscaling.group.AutoScalingGroupConstants;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventory")
public class APIAttachAutoScalingTemplateToGroupEvent extends APIEvent {
    private AutoScalingGroupInventory inventory;

    public AutoScalingGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APIAttachAutoScalingTemplateToGroupEvent(String apiId) {
        super(apiId);
    }

    public APIAttachAutoScalingTemplateToGroupEvent() {
        super(null);
    }

    public static APIAttachAutoScalingTemplateToGroupEvent __example__() {
        APIAttachAutoScalingTemplateToGroupEvent evt = new APIAttachAutoScalingTemplateToGroupEvent();

        AutoScalingGroupInventory inv = new AutoScalingGroupInventory();
        inv.setUuid(uuid());
        inv.setName("test-group");
        inv.setDescription("just for test");
        //inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setAttachedTemplates(new ArrayList<>());
        inv.getAttachedTemplates().add(uuid());

        evt.setInventory(inv);
        return evt;
    }
}
