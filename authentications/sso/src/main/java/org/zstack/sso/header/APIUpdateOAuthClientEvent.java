package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@RestResponse(allTo = "inventory")
public class APIUpdateOAuthClientEvent extends APIEvent {
    private OAuth2ClientInventory inventory;
    public APIUpdateOAuthClientEvent() {
        super(null);
    }

    public APIUpdateOAuthClientEvent(String apiId) {
        super(apiId);
    }

    public OAuth2ClientInventory getInventory() {
        return inventory;
    }

    public void setInventory(OAuth2ClientInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateOAuthClientEvent __example__() {
        APIUpdateOAuthClientEvent evt = new APIUpdateOAuthClientEvent();
        evt.setInventory(OAuth2ClientInventory.__example__());
        return evt;
    }
}

