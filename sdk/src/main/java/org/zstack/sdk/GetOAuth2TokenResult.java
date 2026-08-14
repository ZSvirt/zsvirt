package org.zstack.sdk;

import org.zstack.sdk.OAuth2TokenInventory;

public class GetOAuth2TokenResult {
    public OAuth2TokenInventory inventory;
    public void setInventory(OAuth2TokenInventory inventory) {
        this.inventory = inventory;
    }
    public OAuth2TokenInventory getInventory() {
        return this.inventory;
    }

}
