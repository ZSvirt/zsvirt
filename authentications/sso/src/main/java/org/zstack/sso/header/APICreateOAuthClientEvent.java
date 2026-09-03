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
        OAuth2ClientInventory inv = new OAuth2ClientInventory();
        inv.setUuid(uuid());
        inv.setClientId(uuid());
        inv.setAuthorizationUrl("http://zstack.com/code");
        inv.setTokenUrl("http://zstack.com/token");
        inv.setLoginMNUrl("http://127.0.0.1:8080/zstack/sso/oauth2/");
        inv.setUserinfoUrl("http://zstack.com/userinfoUrl");
        inv.setRedirectUrl("http://zstack.com/redirectUrl");
        evt.setInventory(inv);
        return evt;
    }

}
