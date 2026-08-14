package org.zstack.sdk;

import org.zstack.sdk.OAuth2ClientInventory;

public class CreateOAuthClientResult {
    public OAuth2ClientInventory inventory;
    public void setInventory(OAuth2ClientInventory inventory) {
        this.inventory = inventory;
    }
    public OAuth2ClientInventory getInventory() {
        return this.inventory;
    }

}
