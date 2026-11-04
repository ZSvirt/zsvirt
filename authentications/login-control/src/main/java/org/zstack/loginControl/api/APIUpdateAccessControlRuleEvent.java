package org.zstack.loginControl.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.loginControl.entity.AccessControlRuleInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateAccessControlRuleEvent extends APIEvent {
    private AccessControlRuleInventory inventory;

    public APIUpdateAccessControlRuleEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateAccessControlRuleEvent() {
        super(null);
    }


    public AccessControlRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessControlRuleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateAccessControlRuleEvent __example__() {
        APIUpdateAccessControlRuleEvent evt = new APIUpdateAccessControlRuleEvent();
        return evt;
    }
}
