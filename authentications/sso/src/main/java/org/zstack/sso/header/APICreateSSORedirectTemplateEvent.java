package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@RestResponse(allTo = "inventory")
public class APICreateSSORedirectTemplateEvent extends APIEvent {
    private SSORedirectTemplateInventory inventory;

    public APICreateSSORedirectTemplateEvent() {
    }

    public APICreateSSORedirectTemplateEvent(String apiId) {
        super(apiId);
    }

    public SSORedirectTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(SSORedirectTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSSORedirectTemplateEvent __example__() {
        APICreateSSORedirectTemplateEvent evt = new APICreateSSORedirectTemplateEvent();
        evt.setInventory(SSORedirectTemplateInventory.__example__());
        return evt;
    }
}
