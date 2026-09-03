package org.zstack.loginControl.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.loginControl.entity.AccessControlRuleInventory;

@RestResponse(allTo = "inventory")
public class APIAddAccessControlRuleEvent extends APIEvent {
    private AccessControlRuleInventory inventory;

    public APIAddAccessControlRuleEvent(String apiId) {
        super(apiId);
    }

    public APIAddAccessControlRuleEvent() {
        super(null);
    }

    public AccessControlRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessControlRuleInventory inventory) {
        this.inventory = inventory;
    }
}
