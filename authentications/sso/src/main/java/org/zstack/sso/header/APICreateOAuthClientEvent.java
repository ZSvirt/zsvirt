package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@RestResponse(allTo = "inventory")
public class APICreateOAuthClientEvent extends APIEvent {
    private OAuth2ClientInventory inventory;

    public APICreateOAuthClientEvent() {
        super(null);
    }

    public APICreateOAuthClientEvent(String apiId) {
        super(apiId);
    }

    public OAuth2ClientInventory getInventory() {
        return inventory;
    }

    public void setInventory(OAuth2ClientInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateOAuthClientEvent __example__() {
        APICreateOAuthClientEvent evt = new APICreateOAuthClientEvent();
        evt.setInventory(OAuth2ClientInventory.__example__());
        return evt;
    }

}
