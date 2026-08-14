package org.zstack.sdk;

import org.zstack.sdk.SSORedirectTemplateInventory;

public class UpdateSSORedirectTemplateResult {
    public SSORedirectTemplateInventory inventory;
    public void setInventory(SSORedirectTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public SSORedirectTemplateInventory getInventory() {
        return this.inventory;
    }

}
