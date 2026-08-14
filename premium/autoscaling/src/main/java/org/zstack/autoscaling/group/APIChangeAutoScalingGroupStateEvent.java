package org.zstack.autoscaling.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageState;

import java.util.Collections;

@RestResponse(allTo = "inventory")
public class APIChangeAutoScalingGroupStateEvent extends APIEvent {
    private AutoScalingGroupInventory inventory;

    public APIChangeAutoScalingGroupStateEvent(String apiId) {
        super(apiId);
    }

    public APIChangeAutoScalingGroupStateEvent() {
        super(null);
    }

    public AutoScalingGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AutoScalingGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeAutoScalingGroupStateEvent __example__() {
        APIChangeAutoScalingGroupStateEvent event = new APIChangeAutoScalingGroupStateEvent();

        AutoScalingGroupInventory inventory = new AutoScalingGroupInventory();
        inventory.setState(AutoScalingGroupState.Enabled.toString());
        return event;
    }

}
