package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


@RestResponse(allTo = "inventory")
public class APIUpdateSecurityGroupRulePriorityEvent extends APIEvent {
    /**
     * @desc :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;
    public APIUpdateSecurityGroupRulePriorityEvent() {
    }

    public APIUpdateSecurityGroupRulePriorityEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateSecurityGroupRulePriorityEvent __example__() {
        APIUpdateSecurityGroupRulePriorityEvent event = new APIUpdateSecurityGroupRulePriorityEvent();
        SecurityGroupInventory inventory = new SecurityGroupInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        event.setSuccess(true);
        return event;
    }

}
